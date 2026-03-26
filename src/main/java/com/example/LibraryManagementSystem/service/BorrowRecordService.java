package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordRequest;
import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordResponse;
import com.example.LibraryManagementSystem.dto.mapper.BorrowRecordMapper;
import com.example.LibraryManagementSystem.exception.*;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.model.Users;
import com.example.LibraryManagementSystem.repository.BookRepository;
import com.example.LibraryManagementSystem.repository.BorrowRecordRepository;
import com.example.LibraryManagementSystem.repository.MemberRepository;
import com.example.LibraryManagementSystem.repository.UsersRepository;
import com.example.LibraryManagementSystem.specification.BorrowRecordSpecification;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowRecordService {

    private final BorrowRecordRepository borrowRecordRepository;

    private final MemberRepository memberRepository;

    private final BookRepository bookRepository;

    private final BorrowRecordMapper borrowRecordMapper;

    private final UsersRepository usersRepository;

    private final EmailService emailService;

    private static final Set<String> ALLOWED_SORT_FIELDS  = Set.of(
            "id", "borrowDate", "dueDate", "lateFee", "returnDate", "status"
    );

    public Page<BorrowRecordResponse> getAllBorrowRecords(
            String  memberName,
            String bookName,
            Boolean isArchived,
            String archivedBy,
            BorrowRecord.BorrowStatus status,
            LocalDate borrowedAfter,
            LocalDate borrowedBefore,
            LocalDate dueAfter,
            LocalDate dueBefore,
            BigDecimal minLateFee,
            BigDecimal maxLateFee,
            int pageNo,
            int pageSize,
            String sortBy,
            String sortDir
    ) {

        pageSize = Math.min(pageSize, 50);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)){
            sortBy = "id";
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Specification<BorrowRecord> spec =BorrowRecordSpecification.filterBorrowRecords(
                memberName, bookName, isArchived, archivedBy, status, borrowedAfter, borrowedBefore, dueAfter, dueBefore, minLateFee, maxLateFee
        );

        Page<BorrowRecord> borrowRecordPage = borrowRecordRepository.findAll(spec, pageable);
        return borrowRecordPage.map(borrowRecordMapper::toResponse);
    }

    public BorrowRecordResponse getBorrowRecordById(Long borrowRecordId) {
        log.info("Fetching borrow record by ID: {}", borrowRecordId);
        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(()-> {
                    log.warn("Borrow record not found - ID: {}", borrowRecordId);
                    return new ResourceNotFoundException("BorrowRecord","id",borrowRecordId);
                });
        return borrowRecordMapper.toResponse(borrowRecord);
    }

    @Transactional
    public BorrowRecordResponse addBorrowRecord(BorrowRecordRequest request, String currentUsername) {

        log.info("Processing borrow request - Member ID: {}, Book ID: {}", request.getMemberId(), request.getBookId());

        //checking members
        if (request.getMemberId() == null) {
            throw new ResourceNotFoundException("Member details are missing");
        }
        //checking books
        if (request.getBookId() == null) {
            throw new ResourceNotFoundException("Book details are missing");
        }

        //fetching member
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> {
                    log.error("Member not found - ID: {}", request.getMemberId());
                    return new ResourceNotFoundException("Member", "id", request.getMemberId());
                });


        //checking if current user is member, if he tries to borrow with other memberId
        Users currentUser = usersRepository.findByUsername(currentUsername)
                .orElseThrow(()-> {
                    log.warn("User not found - username: {}", currentUsername);
                    return new ResourceNotFoundException("User Not Found");
                });

        // If user is MEMBER, checking ownership
        if (currentUser.getRole() == Users.Role.MEMBER){
            if(!member.getEmail().equals(currentUser.getEmail())){
                log.warn("Security violation - User {} tried to borrow for member {}", currentUsername, member.getId());
                throw new AccessDeniedException("You can only borrow book with your id");
            }
        }

        //Validating borrow limit based on membership tier
        validatingBorrowingLimit(member);

        //fetching books
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> {
                    log.warn("Book not found - ID: {}", request.getBookId());
                    return new ResourceNotFoundException("Book", "id", request.getBookId());
                });

        //checking book availability
        if (!book.getAvailable() || book.getAvailableCopies() <= 0) {
            log.warn("Book unavailable - Title: '{}', Available: {}", book.getTitle(), book.getAvailableCopies());
            throw new BookNotAvailableException("No copies available");
        }

        //Check if member already has this book active
        boolean alreadyBorrowed = borrowRecordRepository.existsByMemberIdAndBookIdAndStatusAndIsArchivedFalse(
                request.getMemberId(),
                request.getBookId(),
                BorrowRecord.BorrowStatus.ACTIVE
        );

        if (alreadyBorrowed) {
            log.warn("Borrow failed - Member {} already has book '{}'", member.getName(), book.getTitle());
            throw new DuplicateBorrowException("Member already has an active borrow for '" + book.getTitle() + "'");
        }

        //using mapper to convert requestDTO to entity
        BorrowRecord borrowRecord = borrowRecordMapper.toEntity(request);

        //setting member, books, DueDate
        borrowRecord.setMember(member);
        borrowRecord.setBook(book);
        borrowRecord.setDueDate(LocalDate.now().plusDays(member.getBorrowPeriodDays()));

        //if available decrementing
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        if(book.getAvailableCopies() == 0){
            book.setAvailable(false);
        }
        bookRepository.save(book);

        BorrowRecord savedBorrowRecord =  borrowRecordRepository.save(borrowRecord);
        log.info("Book borrowed successfully - Record ID: {}, Member: {}, Book: '{}', Due: {}",
                savedBorrowRecord.getId(), savedBorrowRecord.getMember().getName(), savedBorrowRecord.getBook().getTitle(), savedBorrowRecord.getDueDate());

        //Send borrow confirmation email
        emailService.sendBorrowConfirmation(savedBorrowRecord);

        return borrowRecordMapper.toResponse(savedBorrowRecord);
    }

    @Transactional
    public BorrowRecordResponse updateBorrowRecord(Long borrowRecordId, BorrowRecordRequest borrowRecordRequest) {

        log.info("Processing update borrow record - ID: {}", borrowRecordId);

        BorrowRecord existingRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(()-> {
                    log.error("Borrow record not found - ID: {}", borrowRecordId);
                    return new ResourceNotFoundException("BorrowRecord","id",borrowRecordId);
                });

        //not allowing updating a RETURNED record
        if (existingRecord.getStatus() == BorrowRecord.BorrowStatus.RETURNED) {
            log.warn("Update failed - Record returned. ID: {}, status: {}", borrowRecordId, existingRecord.getStatus());
            throw new ConflictException("Cannot update a returned borrow record");
        }

        if(borrowRecordRequest.getMemberId() != null){
            Member member = memberRepository.findById(borrowRecordRequest.getMemberId())
                    .orElseThrow(()-> new ResourceNotFoundException("Member", "id", borrowRecordRequest.getMemberId()));
            existingRecord.setMember(member);
        }

        if(borrowRecordRequest.getBookId() != null){
            Book book = bookRepository.findById(borrowRecordRequest.getBookId())
                    .orElseThrow(()-> new ResourceNotFoundException("Book", "id", borrowRecordRequest.getBookId()));
            existingRecord.setBook(book);
        }

        BorrowRecord savedBorrowRecord =  borrowRecordRepository.save(existingRecord);
        log.warn("Record updated successfully - Record ID: {}, Member: {}, Book: '{}', Due: {}",
                savedBorrowRecord.getId(), savedBorrowRecord.getMember().getName(), savedBorrowRecord.getBook().getTitle(), savedBorrowRecord.getDueDate());

        return borrowRecordMapper.toResponse(savedBorrowRecord);
    }

    @Transactional
    public BorrowRecordResponse processReturn(Long borrowRecordId, String currentUsername) {

        log.info("Processing return - Record ID: {}, User: {}", borrowRecordId, currentUsername);

        BorrowRecord existingRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(()-> {
                    log.warn("Borrow record not found - ID: {}", borrowRecordId);
                    return new ResourceNotFoundException("BorrowRecord","id",borrowRecordId);
                });

        Users currentUser = usersRepository.findByUsername(currentUsername)
                .orElseThrow(()-> {
                    log.warn("User not found - username: {}", currentUsername);
                    return new ResourceNotFoundException("User Not Found");
                });

        // If user is MEMBER, checking ownership
        if (currentUser.getRole() == Users.Role.MEMBER){
            if(!existingRecord.getMember().getEmail().equals(currentUser.getEmail())){
                log.warn("Security violation - User {} tried to return record {}",
                        currentUsername, borrowRecordId);
                throw new AccessDeniedException("You can only return your own books");
            }
        }

        if(existingRecord.getStatus() == BorrowRecord.BorrowStatus.RETURNED) {
            log.warn("Already returned - Record: {}", borrowRecordId);
            throw new ConflictException("This book has already been returned");
        }

        Book book = existingRecord.getBook();
        existingRecord.setReturnDate(LocalDateTime.now());
        existingRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);

        book.setAvailableCopies(book.getAvailableCopies() + 1);
        if(book.getAvailableCopies() > 0){
            book.setAvailable(true);
        }
        bookRepository.save(book);

        BorrowRecord savedBorrowRecord =  borrowRecordRepository.save(existingRecord);
        log.info("Book returned - Record ID: {}, Member ID: {}, Book: {}",
                borrowRecordId, existingRecord.getMember().getId(), existingRecord.getBook().getTitle());
        //Send return confirmation email
        emailService.sendReturnConfirmation(savedBorrowRecord);

        return borrowRecordMapper.toResponse(savedBorrowRecord);
    }

    @Transactional
    public BorrowRecordResponse archiveBorrowRecord(Long id, String archivedBy, String reason){
        BorrowRecord borrowRecord = borrowRecordRepository.findById(id)
                .orElseThrow(()-> {
                    log.error("Borrow record not found - ID: {}", id);
                    return new ResourceNotFoundException("BorrowRecord", "id", id);
                });

        if(borrowRecord.getIsArchived()){
            log.warn("Archive failed - Record already archived. ID: {}, archivedAt: {}",
                    id, borrowRecord.getArchivedAt());
            throw new ConflictException("BorrowRecord is already archived");
        }
        if(borrowRecord.getStatus() != BorrowRecord.BorrowStatus.RETURNED){
            log.warn("Archive failed - active borrow record. ID: {}, status: {}",
                    id, borrowRecord.getStatus());
            throw new ActiveBorrowExistsException("Cannot archive active borrow record. Please return the book first.");
        }
        borrowRecord.setIsArchived(true);
        borrowRecord.setArchivedAt(LocalDateTime.now());
        borrowRecord.setArchivedBy(archivedBy != null ? archivedBy : "SYSTEM");
        borrowRecord.setArchiveReason(reason);

        BorrowRecord savedBorrowRecord = borrowRecordRepository.save(borrowRecord);
        log.info("Record archived successfully - ID: {}, book: '{}'",
                savedBorrowRecord.getId(), savedBorrowRecord.getBook().getTitle());
        return borrowRecordMapper.toResponse(savedBorrowRecord);
    }

    @Transactional
    public void deleteBorrowRecord(Long borrowRecordId) {

        log.info("Deleting record - ID: {}", borrowRecordId);

        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(()-> {
                    log.error("Borrow record not found - ID: {}", borrowRecordId);
                    return new ResourceNotFoundException("BorrowRecord", "id", borrowRecordId);
                });

        if(borrowRecord.getStatus() != BorrowRecord.BorrowStatus.RETURNED){
            log.warn("Delete failed - active borrow record. ID: {}, status: {}",
                    borrowRecordId, borrowRecord.getStatus());
            throw new ActiveBorrowExistsException( "Cannot delete active borrow record. Please return the book first.");
        }
        if(!borrowRecord.getIsArchived()){
            log.warn("Delete failed - borrow record: {} is not archived. status: {}",
                    borrowRecordId, borrowRecord.getStatus());
            throw new ConflictException("BorrowRecord cannot be deleted. Only archived records can be deleted.");
        }
        borrowRecordRepository.delete(borrowRecord);
        log.info("Borrow record deleted successfully - ID: {}", borrowRecordId);
    }

    //helping methods
    private void validatingBorrowingLimit(Member member){
        Long activeBorrows = borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(
                member.getId(),
                BorrowRecord.BorrowStatus.ACTIVE
        );

        if (activeBorrows >= member.getMaxBooksAllowed()){
            log.warn("Borrow failed - Member {} has reached limit. Max: {}",member.getName(), member.getMaxBooksAllowed());
            throw new BorrowLimitExceededException(member.getMaxBooksAllowed(), member.getMembershipType().name());
        }
    }

    //scheduler calls for every 1  hour
    @Transactional
    public void markOverdueRecords(){

        log.info("Running scheduled task: Mark Overdue Records");

        List<BorrowRecord> activeRecords  = borrowRecordRepository.
                findByStatusAndDueDateBefore(BorrowRecord.BorrowStatus.ACTIVE, LocalDate.now());

        if (activeRecords.isEmpty()) {
            log.info("No active records to mark as overdue");
            return;
        }

        //activeRecords.forEach(BorrowRecord::markOverDue);
        activeRecords.forEach(record -> {
            record.setStatus(BorrowRecord.BorrowStatus.OVERDUE);
            record.setLateFee(record.calculateLateFee());

            //send overdue reminder email
            emailService.sendOverdueReminder(record);
        });
        borrowRecordRepository.saveAll(activeRecords);
        log.info("Marked {} records as OVERDUE", activeRecords.size());
    }

    //scheduler calls for every 1 hour : 1min
    @Transactional
    public void updateOverdueRecords(){

        log.info("Running scheduled task: Update Late Fees & Send Reminders");

        List<BorrowRecord> overdueRecords  = borrowRecordRepository.
                findByStatus(BorrowRecord.BorrowStatus.OVERDUE);

        if (overdueRecords.isEmpty()) {
            log.info("No overdue records to process");
            return;
        }
        overdueRecords.forEach(record -> {
            record.setLateFee(record.calculateLateFee());

            //send overdue reminder email
            emailService.sendOverdueReminder(record);
        });
        borrowRecordRepository.saveAll(overdueRecords);

        log.info("Updated {} overdue records",
                overdueRecords.size());
    }

    public Page<BorrowRecordResponse> getMyBorrowRecords(String username,
                                                         int pageNo,
                                                         int pageSize,
                                                         String sortBy,
                                                         String sortDir) {

        pageSize = Math.min(pageSize, 50);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)){
            sortBy = "borrowDate";
        }
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<BorrowRecord> borrowRecordPage = borrowRecordRepository.findAllByMemberName(username, pageable);

        log.info("Found {} borrow records for user: {}", borrowRecordPage.getTotalElements(), username);

        return borrowRecordPage.map(borrowRecordMapper::toResponse);
    }
}


