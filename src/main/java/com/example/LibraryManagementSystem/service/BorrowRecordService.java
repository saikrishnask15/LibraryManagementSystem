package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordRequest;
import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordResponse;
import com.example.LibraryManagementSystem.dto.mapper.BorrowRecordMapper;
import com.example.LibraryManagementSystem.exception.ActiveBorrowExistsException;
import com.example.LibraryManagementSystem.exception.ConflictException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.repository.BookRepository;
import com.example.LibraryManagementSystem.repository.BorrowRecordRepository;
import com.example.LibraryManagementSystem.repository.MemberRepository;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowRecordService {

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;


    public List<BorrowRecordResponse> getAllBorrowRecords() {
        return borrowRecordMapper.toResponseList(borrowRecordRepository.findAll());
    }

    public BorrowRecordResponse getBorrowRecordById(Long borrowRecordId) {
        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(()-> new ResourceNotFoundException("BorrowRecord","id",borrowRecordId));
        return borrowRecordMapper.toResponse(borrowRecord);
    }

    public BorrowRecordResponse addBorrowRecord(BorrowRecordRequest request) {
        //using mapper to convert requestDTO to entity
        BorrowRecord borrowRecord = borrowRecordMapper.toEntity(request);

        //checking members
        if(borrowRecord.getMember() != null && borrowRecord.getMember().getId() != null){
            Integer memberId = borrowRecord.getMember().getId();

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(()-> new ResourceNotFoundException("Member","id",memberId));
            borrowRecord.setMember(member);
        }else{
            throw new ResourceNotFoundException("Member details are missing");
        }

        //checking books
        if(borrowRecord.getBook() != null && borrowRecord.getBook().getId() != null){
            Integer bookId = borrowRecord.getBook().getId();

            Book book = bookRepository.findById(bookId)
                    .orElseThrow(()-> new ResourceNotFoundException("Book","id",bookId));
            borrowRecord.setBook(book);
        }else{
            throw new ResourceNotFoundException("Book details are missing");
        }
        BorrowRecord savedBorrowRecord =  borrowRecordRepository.save(borrowRecord);
        return borrowRecordMapper.toResponse(savedBorrowRecord);
    }

    public BorrowRecordResponse updateBorrowRecord(Long borrowRecordId, BorrowRecordRequest borrowRecordRequest) {
        BorrowRecord existingRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(()-> new ResourceNotFoundException("BorrowRecord","id",borrowRecordId));

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

        if(borrowRecordRequest.getStatus() == BorrowRecord.BorrowStatus.RETURNED){
            existingRecord.setReturnDate(LocalDateTime.now());
            existingRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
        }
        BorrowRecord savedBorrowRecord =  borrowRecordRepository.save(existingRecord);
        return borrowRecordMapper.toResponse(savedBorrowRecord);
    }

    public BorrowRecordResponse processReturn(Long borrowRecordId, BorrowRecordRequest borrowRecordRequest) {
        BorrowRecord existingRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(()-> new ResourceNotFoundException("BorrowRecord","id",borrowRecordId));


        if(borrowRecordRequest.getStatus() == BorrowRecord.BorrowStatus.RETURNED){
            existingRecord.setReturnDate(LocalDateTime.now());
            existingRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
        }
        BorrowRecord savedBorrowRecord =  borrowRecordRepository.save(existingRecord);
        return borrowRecordMapper.toResponse(savedBorrowRecord);
    }

    public BorrowRecordResponse archiveBorrowRecord(Long id, String archivedBy, String reason){
        BorrowRecord borrowRecord = borrowRecordRepository.findById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(borrowRecord.getIsArchived()){
            throw new ConflictException("BorrowRecord is already archived");
        }
        if(borrowRecord.getStatus() != BorrowRecord.BorrowStatus.RETURNED){
            throw new ActiveBorrowExistsException("'Cannot archive active borrow record.' 'Please return the book first.'");
        }
        borrowRecord.setIsArchived(true);
        borrowRecord.setArchivedAt(LocalDateTime.now());
        borrowRecord.setArchivedBy(archivedBy != null ? archivedBy : "SYSTEM");
        borrowRecord.setArchiveReason(reason);

        BorrowRecord savedBorrowRecord = borrowRecordRepository.save(borrowRecord);
        return borrowRecordMapper.toResponse(savedBorrowRecord);
    }


    public void deleteBorrowRecord(Long borrowRecordId) {
        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if(borrowRecord.getStatus() != BorrowRecord.BorrowStatus.RETURNED){
            throw new ActiveBorrowExistsException( "\"Cannot delete active borrow record. \" +\n" +
                    "\"Please return the book first.\"");
        }
        if(!borrowRecord.getIsArchived()){
            throw new ConflictException("BorrowRecord is cannot be deleted, only archived book is deleted");
        }
        borrowRecordRepository.delete(borrowRecord);
    }

    public List<BorrowRecordResponse> getAllArchivedBorrowRecords() {
        return borrowRecordMapper.toResponseList(borrowRecordRepository.findByIsArchivedTrue());
    }
    public List<BorrowRecordResponse> getAllActiveBorrowRecords() {
        return borrowRecordMapper.toResponseList(borrowRecordRepository.findByIsArchivedFalse());
    }


}

