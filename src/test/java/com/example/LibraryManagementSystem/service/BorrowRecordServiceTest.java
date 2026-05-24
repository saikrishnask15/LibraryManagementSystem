package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordRequest;
import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordResponse;
import com.example.LibraryManagementSystem.dto.mapper.BorrowRecordMapper;
import com.example.LibraryManagementSystem.exception.*;
import com.example.LibraryManagementSystem.model.*;
import com.example.LibraryManagementSystem.repository.BookRepository;
import com.example.LibraryManagementSystem.repository.BorrowRecordRepository;
import com.example.LibraryManagementSystem.repository.MemberRepository;
import com.example.LibraryManagementSystem.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BorrowRecordService Unit Tests")
class BorrowRecordServiceTest {

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BorrowRecordService borrowRecordService;

    private Member member;
    private Book book;
    private BorrowRecord borrowRecord;
    private BorrowRecordRequest borrowRecordRequest;
    private BorrowRecordResponse borrowRecordResponse;
    private Users adminUser;
    private Users memberUser;

    @BeforeEach
    void setUp(){
        member = new Member();
        member.setId(1);
        member.setName("Alice");
        member.setEmail("alice@example.com");
        member.setMembershipType(MembershipType.BASIC);

        book = new Book();
        book.setId(1);
        book.setTitle("Harry Potter");
        book.setIsBn("978-0439708180");
        book.setAvailable(true);
        book.setAvailableCopies(5);
        book.setTotalCopies(5);

        borrowRecord = new BorrowRecord();
        borrowRecord.setId(1L);
        borrowRecord.setMember(member);
        borrowRecord.setBook(book);
        borrowRecord.setBorrowDate(LocalDate.now());
        borrowRecord.setDueDate(LocalDate.now().plusDays(14));
        borrowRecord.setStatus(BorrowRecord.BorrowStatus.ACTIVE);
        borrowRecord.setIsArchived(false);
        borrowRecord.setLateFee(BigDecimal.ZERO);

        borrowRecordRequest = new BorrowRecordRequest();
        borrowRecordRequest.setMemberId(1);
        borrowRecordRequest.setBookId(1);

        // DTO out — nested MemberDTO and BookDTO mirror the real response shape
        BorrowRecordResponse.MemberDTO memberDTO = new BorrowRecordResponse.MemberDTO(1, "Alice", "alice@example.com");
        BorrowRecordResponse.BookDTO bookDTO = new BorrowRecordResponse.BookDTO(1, "Harry Potter");

        borrowRecordResponse = new BorrowRecordResponse();
        borrowRecordResponse.setId(1L);
        borrowRecordResponse.setMember(memberDTO);
        borrowRecordResponse.setBook(bookDTO);
        borrowRecordResponse.setStatus(BorrowRecord.BorrowStatus.ACTIVE);
        borrowRecordResponse.setIsArchived(false);
        borrowRecordResponse.setLateFee(BigDecimal.ZERO);

        // ADMIN user — can borrow/return on behalf of any member
        adminUser = new Users();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Users.Role.ADMIN);

        // MEMBER user whose email matches the member fixture
        memberUser = new Users();
        memberUser.setUsername("Alice");
        memberUser.setEmail("alice@example.com");
        memberUser.setRole(Users.Role.MEMBER);
    }

    @Nested
    @DisplayName("getAllBorrowRecords()")
    class GetAllBorrowRecords{

        @Test
        @DisplayName("Returns mapped page when valid params are provided")
        void returnsMappedPage(){
            Page<BorrowRecord> page = new PageImpl<>(List.of(borrowRecord));
            when(borrowRecordRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            Page<BorrowRecordResponse> result = borrowRecordService.getAllBorrowRecords(  null, null, null, null, null, null, null, null, null, null, null,
                    0, 10, "id", "ASC");

            assertEquals(1, result.getContent().size());
            assertEquals("Alice", result.getContent().getFirst().getMember().getName());
            assertEquals("Harry Potter", result.getContent().getFirst().getBook().getTitle());

        }

        @Test
        @DisplayName("Caps pageSize at 50 regardless of requested value")
        void capsPageSizeAt50(){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(borrowRecordRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            borrowRecordService.getAllBorrowRecords(  null, null, null, null, null, null, null, null, null, null, null,
                    0, 999, "id", "ASC");
            assertEquals(50, captor.getValue().getPageSize());
        }

        @ParameterizedTest
        @ValueSource(strings = {"member", "book", "unknown", ""})
        @DisplayName("Falls back to 'id' for disallowed sortBy values")
        void fallsBackToIdForInvalidSortField(String invalidSortBy){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(borrowRecordRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            borrowRecordService.getAllBorrowRecords(  null, null, null, null, null, null, null, null, null, null, null,
                    0, 999, invalidSortBy , "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor("id"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "id", "borrowDate", "dueDate", "lateFee", "returnDate", "status"})
        @DisplayName("Accepts all allowed sortBy fields without falling back")
        void acceptsAllAllowedSortFields(String validSortBy){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(borrowRecordRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());
            borrowRecordService.getAllBorrowRecords( null, null, null, null, null, null, null, null, null, null, null,
                    0, 50, validSortBy , "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor(validSortBy));
        }

        @Test
        @DisplayName("Applies descending sort when sortDir is 'DESC'")
        void appliesDescendingSort(){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            when(borrowRecordRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());
            borrowRecordService.getAllBorrowRecords( null, null, null, null, null, null, null, null, null, null, null,
                    0, 50, "status" , "DESC");

            Sort.Order order = captor.getValue().getSort().getOrderFor("status");
            assertNotNull(order);
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }
    }

    @Nested
    @DisplayName("getBorrowRecordById()")
    class GetBorrowRecordById{

        @Test
        @DisplayName("Returns response with nested member and book DTOs when record exists")
        void returnsResponseWhenFound(){

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            BorrowRecordResponse result = borrowRecordService.getBorrowRecordById(1L);

            assertEquals(1, result.getId());
            assertEquals("Alice", result.getMember().getName());
            assertEquals("alice@example.com", result.getMember().getEmail());
            assertEquals("Harry Potter", result.getBook().getTitle());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when record is missing")
        void throwsWhenNotFound(){
            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    ()->borrowRecordService.getBorrowRecordById(1L));

            verify(borrowRecordMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("addBorrowRecord()")
    class AddBorrowRecord{

        @Test
        @DisplayName("Creates borrow record, decrements copies, sends email — happy path for ADMIN")
        void adminCanBorrowForAnyMember(){
            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.ofNullable(adminUser));
            when(borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(0L);
            when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
            when(borrowRecordRepository.existsByMemberIdAndBookIdAndStatusAndIsArchivedFalse(1, 1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(false);
            when(borrowRecordMapper.toEntity(borrowRecordRequest)).thenReturn(borrowRecord);
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            BorrowRecordResponse result = borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin");

            assertEquals("Alice", result.getMember().getName());
            assertEquals("Harry Potter", result.getBook().getTitle());

            // availableCopies must be decremented by 1
            assertEquals(4,book.getAvailableCopies());
            verify(bookRepository).save(book);

            // Confirmation email must be sent exactly once
            verify(emailService).sendBorrowConfirmation(borrowRecord);
        }

        @Test
        @DisplayName("MEMBER can borrow when their email matches the current user's email")
        void memberCanBorrowForThemself(){

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("Alice")).thenReturn(Optional.ofNullable(memberUser));
            when(borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(0L);
            when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
            when(borrowRecordRepository.existsByMemberIdAndBookIdAndStatusAndIsArchivedFalse(1, 1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(false);
            when(borrowRecordMapper.toEntity(borrowRecordRequest)).thenReturn(borrowRecord);
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            BorrowRecordResponse result = borrowRecordService.addBorrowRecord(borrowRecordRequest, "Alice");

            assertNotNull(result);
            verify(bookRepository).save(book);
            verify(emailService).sendBorrowConfirmation(borrowRecord);
        }

        @Test
        @DisplayName("Throws AccessDeniedException when MEMBER tries to borrow using another member's ID")
        void memberCannotBorrowForOtherMember(){
            // memberUser email is "alice@example.com" but member fixture email is also "alice@example.com"
            // Override member email to simulate a different member
            member.setEmail("bob@example.com");

            when(memberRepository.findById(1)).thenReturn(Optional.ofNullable(member));
            when(usersRepository.findByUsername("Alice")).thenReturn(Optional.ofNullable(memberUser));

            assertThrows(AccessDeniedException.class, ()->borrowRecordService.addBorrowRecord(borrowRecordRequest, "Alice"));

            verify(bookRepository, never()).findById(any());
            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when memberId is null")
        void throwsWhenMemberIdIsNull(){
            borrowRecordRequest.setMemberId(null);

            assertThrows(ResourceNotFoundException.class, ()->borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin"));

            verify(memberRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when bookId is null")
        void throwsWhenBookIdIsNull(){
            borrowRecordRequest.setBookId(null);

            assertThrows(ResourceNotFoundException.class, () -> borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin"));

            verify(bookRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when member does not exist")
        void throwsWhenMemberNotFound(){
            when(memberRepository.findById(1)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin"));

            verify(bookRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when current user is not found in the system")
        void throwsWhenCurrentUserNotFound(){
            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    ()->borrowRecordService.addBorrowRecord(borrowRecordRequest, "ghost"));

            verify(bookRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Throws BorrowLimitExceededException when member has reached their borrow limit")
        void throwsWhenBorrowLimitReached(){
            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            // activeBorrows == maxBooksAllowed → limit reached
            when(borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(3L);

            assertThrows(BorrowLimitExceededException.class,() -> borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin"));

            verify(bookRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when book does not exist")
        void throwsWhenBookNotFound(){
            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(0L);
            when(bookRepository.findById(1)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    ()->borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin"));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws BookNotAvailableException when book has zero available copies")
        void throwsWhenBookHasNoAvailableCopies(){
            book.setAvailableCopies(0);
            book.setAvailable(false);


            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(0L);
            when(bookRepository.findById(1)).thenReturn(Optional.of(book));

            assertThrows(BookNotAvailableException.class, ()->borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin"));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws BookNotAvailableException when book.available flag is false even with copies > 0")
        void throwsWhenBookAvailableFlagIsFalse(){
            book.setAvailable(false); // flag is false regardless of availableCopies
            book.setAvailableCopies(3);

            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(0L);
            when(bookRepository.findById(1)).thenReturn(Optional.of(book));

            assertThrows(BookNotAvailableException.class, ()->borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin"));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws DuplicateBorrowException when member already has this book active")
        void throwsWhenMemberAlreadyHasBookActive(){

            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(0L);
            when(bookRepository.findById(1)).thenReturn(Optional.of(book));
            when(borrowRecordRepository.existsByMemberIdAndBookIdAndStatusAndIsArchivedFalse(1, 1, BorrowRecord.BorrowStatus.ACTIVE)).thenReturn(true);

            assertThrows(DuplicateBorrowException.class, ()->borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin"));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sets available=false when last copy is borrowed")
        void setsBookUnavailableWhenLastCopyBorrowed(){

            book.setAvailableCopies(1); // last copy
            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(0L);
            when(bookRepository.findById(1)).thenReturn(Optional.of(book));
            when(borrowRecordRepository.existsByMemberIdAndBookIdAndStatusAndIsArchivedFalse(
                    1, 1, BorrowRecord.BorrowStatus.ACTIVE)).thenReturn(false);
            when(borrowRecordMapper.toEntity(borrowRecordRequest)).thenReturn(borrowRecord);
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin");

            // availableCopies hits 0 → available must be set to false
            assertEquals(0, book.getAvailableCopies());
            assertFalse(book.getAvailable());
        }

        @Test
        @DisplayName("Sets dueDate based on member's MembershipType borrowPeriodDays")
        void setsDueDateFromMemberBorrowPeriod(){
            // Switch to PREMIUM (30 days) to prove the period comes from the enum, not a hardcoded value
            member.setMembershipType(MembershipType.PREMIUM);

            when(memberRepository.findById(1)).thenReturn(Optional.of(member));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(borrowRecordRepository.countByMemberIdAndStatusAndIsArchivedFalse(1, BorrowRecord.BorrowStatus.ACTIVE))
                    .thenReturn(0L);
            when(bookRepository.findById(1)).thenReturn(Optional.of(book));
            when(borrowRecordRepository.existsByMemberIdAndBookIdAndStatusAndIsArchivedFalse(
                    1, 1, BorrowRecord.BorrowStatus.ACTIVE)).thenReturn(false);
            when(borrowRecordMapper.toEntity(borrowRecordRequest)).thenReturn(borrowRecord);
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            borrowRecordService.addBorrowRecord(borrowRecordRequest, "admin");

            // dueDate = today + member.getMembershipType().getBorrowPeriodDays() = today + 30
            assertEquals(borrowRecord.getDueDate(), LocalDate.now().plusDays(member.getMembershipType().getBorrowPeriodDays()));
        }
    }

    @Nested
    @DisplayName("updateBorrowRecord()")
    class UpdateBorrowRecord{

        @Test
        @DisplayName("Throws ResourceNotFoundException when record to update does not exist")
        void throwsWhenRecordNotFound(){

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->borrowRecordService.updateBorrowRecord(1L, borrowRecordRequest));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ConflictException when trying to update a RETURNED record")
        void throwsWhenRecordAlreadyReturned(){

            borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));

            assertThrows(ConflictException.class, ()->borrowRecordService.updateBorrowRecord(1L, borrowRecordRequest));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Updates member when a valid memberId is provided in request")
        void updatesMemberWhenProvided(){

            Member newMember = new Member();
            newMember.setId(2);
            newMember.setName("Bob");

            borrowRecordRequest.setMemberId(2);
            borrowRecordRequest.setBookId(null);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(memberRepository.findById(2)).thenReturn(Optional.of(newMember));
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            borrowRecordService.updateBorrowRecord(1L, borrowRecordRequest);

            assertEquals(newMember, borrowRecord.getMember());
            verify(borrowRecordRepository).save(borrowRecord);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when new memberId does not exist")
        void throwsWhenNewMemberNotFound(){

            borrowRecordRequest.setMemberId(99);
            borrowRecordRequest.setBookId(null);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(memberRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->borrowRecordService.updateBorrowRecord(1L, borrowRecordRequest));
            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Updates book when a valid bookId is provided in request")
        void updatesBookWhenProvided(){

            Book newBook = new Book();
            newBook.setId(2);
            newBook.setTitle("Dune");

            borrowRecordRequest.setMemberId(null);
            borrowRecordRequest.setBookId(2);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(bookRepository.findById(2)).thenReturn(Optional.of(newBook));
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            borrowRecordService.updateBorrowRecord(1L, borrowRecordRequest);

            assertEquals(newBook, borrowRecord.getBook());
            verify(borrowRecordRepository).save(borrowRecord);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when new bookId does not exist")
        void throwsWhenNewBookNotFound(){

            borrowRecordRequest.setMemberId(null);
            borrowRecordRequest.setBookId(99);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(bookRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    ()->borrowRecordService.updateBorrowRecord(1L, borrowRecordRequest));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Skips member and book lookup when both IDs in request are null")
        void skipsLookupsWhenBothIdsAreNull(){

            borrowRecordRequest.setMemberId(null);
            borrowRecordRequest.setBookId(null);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            borrowRecordService.updateBorrowRecord(1L, borrowRecordRequest);

            verify(memberRepository, never()).findById(any());
            verify(bookRepository, never()).findById(any());
            verify(borrowRecordRepository).save(borrowRecord);
        }
    }

    @Nested
    @DisplayName("processReturn()")
    class ProcessReturn{

        @Test
        @DisplayName("Processes return, increments copies, sends email — happy path for ADMIN")
        void adminCanReturnAnyBook(){

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.ofNullable(adminUser));
            when(bookRepository.save(book)).thenReturn(book);
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            book.setAvailableCopies(4); // 1 copy was borrowed

            borrowRecordService.processReturn(1L, "admin");

            // availableCopies incremented back to 5
            assertEquals(5, borrowRecord.getBook().getAvailableCopies());
            assertTrue(book.getAvailable());
            assertEquals(BorrowRecord.BorrowStatus.RETURNED, borrowRecord.getStatus());
            assertNotNull(borrowRecord.getReturnDate());
            verify(emailService).sendReturnConfirmation(borrowRecord);
        }

        @Test
        @DisplayName("MEMBER can return their own book when email matches")
        void memberCanReturnOwnBook(){
            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(usersRepository.findByUsername("Alice")).thenReturn(Optional.ofNullable(memberUser));
            when(bookRepository.save(book)).thenReturn(book);
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            book.setAvailableCopies(4); // 1 copy was borrowed

            borrowRecordService.processReturn(1L, "Alice");

            // availableCopies incremented back to 5
            assertEquals(5, borrowRecord.getBook().getAvailableCopies());
            assertTrue(book.getAvailable());
            assertEquals(BorrowRecord.BorrowStatus.RETURNED, borrowRecord.getStatus());
            assertNotNull(borrowRecord.getReturnDate());
            verify(emailService).sendReturnConfirmation(borrowRecord);
        }

        @Test
        @DisplayName("Throws AccessDeniedException when MEMBER tries to return another member's book")
        void memberCannotReturnOtherMembersBook(){
            // memberUser email is "alice@example.com" but record belongs to "bob@example.com"
            member.setEmail("bob@example.com");

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(usersRepository.findByUsername("Alice")).thenReturn(Optional.ofNullable(memberUser));

            assertThrows(AccessDeniedException.class,
                    ()->borrowRecordService.processReturn(1L, "Alice"));

            verify(bookRepository, never()).save(any());
            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when current user is not found")
        void throwsWhenCurrentUserNotFound() {
            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));
            when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,() -> borrowRecordService.processReturn(1L, "ghost"));
        }

        @Test
        @DisplayName("Throws ConflictException when book has already been returned")
        void throwsWhenAlreadyReturned(){

            borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

            assertThrows(ConflictException.class, ()->borrowRecordService.processReturn(1L, "admin"));

            verify(bookRepository, never()).save(any());
            verify(emailService, never()).sendReturnConfirmation(any());
        }

        @Test
        @DisplayName("Sets available=true when first copy is returned to an empty shelf")
        void setsBookAvailableWhenFirstCopyReturned(){
            book.setAvailableCopies(0);
            book.setAvailable(false);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));
            when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
            when(bookRepository.save(book)).thenReturn(book);
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);
            borrowRecordService.processReturn(1L, "admin");

            assertEquals(1, book.getAvailableCopies());
            assertEquals(true, book.getAvailable());
        }
    }

    @Nested
    @DisplayName("archiveBorrowRecord()")
    class ArchiveBorrowRecord{

        @Test
        @DisplayName("Archives a RETURNED record successfully with provided reason and archivedBy")
        void archivesReturnedRecord(){

            borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
            borrowRecord.setIsArchived(false);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            borrowRecordService.archiveBorrowRecord(1L, "admin", "Annual cleanup");

            assertTrue(borrowRecord.getIsArchived());
            assertEquals("admin", borrowRecord.getArchivedBy());
            assertEquals("Annual cleanup", borrowRecord.getArchiveReason());
            assertNotNull(borrowRecord.getArchivedAt());
        }

        @Test
        @DisplayName("Defaults archivedBy to 'SYSTEM' when null is passed")
        void defaultsArchivedByToSystemWhenNull(){
            borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
            borrowRecord.setIsArchived(false);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));
            when(borrowRecordRepository.save(borrowRecord)).thenReturn(borrowRecord);
            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            borrowRecordService.archiveBorrowRecord(1L, null, null);

            assertTrue(borrowRecord.getIsArchived());
            assertEquals("SYSTEM", borrowRecord.getArchivedBy());
            assertNotNull(borrowRecord.getArchivedAt());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when record to archive does not exist")
        void throwsWhenRecordNotFound(){
            when(borrowRecordRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->borrowRecordService.archiveBorrowRecord(99L, null , null));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ConflictException when record is already archived")
        void throwsWhenAlreadyArchived(){
            borrowRecord.setIsArchived(true);  // already archived

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));

            assertThrows(ConflictException.class, ()->borrowRecordService.archiveBorrowRecord(1L, null, null));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ActiveBorrowExistsException when record is ACTIVE (not yet returned)")
        void throwsWhenRecordIsActive(){

            borrowRecord.setStatus(BorrowRecord.BorrowStatus.ACTIVE);
            borrowRecord.setIsArchived(false);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));

            assertThrows(ActiveBorrowExistsException.class, ()->borrowRecordService.archiveBorrowRecord(1L, null, null));

            verify(borrowRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ActiveBorrowExistsException when record is OVERDUE (not yet returned)")
        void throwsWhenRecordIsOverdue(){
            borrowRecord.setStatus(BorrowRecord.BorrowStatus.OVERDUE);
            borrowRecord.setIsArchived(false);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));

            assertThrows(ActiveBorrowExistsException.class, () -> borrowRecordService.archiveBorrowRecord(1L, "admin", "reason"));

            verify(borrowRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteBorrowRecord()")
    class DeleteBorrowRecord{

        @Test
        @DisplayName("Deletes record when it is RETURNED and archived")
        void deletesReturnedAndArchivedRecord(){
            borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
            borrowRecord.setIsArchived(true);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));

            borrowRecordService.deleteBorrowRecord(1L);

            verify(borrowRecordRepository).delete(borrowRecord);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when record to delete does not exist")
        void throwsWhenRecordNotFound(){
            when(borrowRecordRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->borrowRecordService.deleteBorrowRecord(99L));

            verify(borrowRecordRepository, never()).delete((BorrowRecord) any());
        }

        @Test
        @DisplayName("Throws ActiveBorrowExistsException when record is ACTIVE")
        void throwsWhenRecordIsActive(){
            borrowRecord.setStatus(BorrowRecord.BorrowStatus.ACTIVE);
            borrowRecord.setIsArchived(false);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.ofNullable(borrowRecord));

            assertThrows(ActiveBorrowExistsException.class, ()->borrowRecordService.deleteBorrowRecord(1L));

            verify(borrowRecordRepository, never()).delete((BorrowRecord) any());
        }

        @Test
        @DisplayName("Throws ActiveBorrowExistsException when record is OVERDUE")
        void throwsWhenRecordIsOverdue(){
            borrowRecord.setStatus(BorrowRecord.BorrowStatus.OVERDUE);
            borrowRecord.setIsArchived(false);

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));

            assertThrows(ActiveBorrowExistsException.class, ()->borrowRecordService.deleteBorrowRecord(1L));

            verify(borrowRecordRepository, never()).delete((BorrowRecord) any());
        }

        @Test
        @DisplayName("Throws ConflictException when record is RETURNED but NOT archived")
        void throwsWhenReturnedButNotArchived(){
            borrowRecord.setStatus(BorrowRecord.BorrowStatus.RETURNED);
            borrowRecord.setIsArchived(false); // returned but not archived yet

            when(borrowRecordRepository.findById(1L)).thenReturn(Optional.of(borrowRecord));

            assertThrows(ConflictException.class, ()->borrowRecordService.deleteBorrowRecord(1L));
            verify(borrowRecordRepository, never()).delete((BorrowRecord) any());
        }
    }

    @Nested
    @DisplayName("markOverdueRecords()")
    class MarkOverdueRecords{

        @Test
        @DisplayName("Marks ACTIVE overdue records as OVERDUE, sets late fee, sends reminder emails")
        void marksActiveRecordsAsOverdue(){
            BorrowRecord overdueRecord = new BorrowRecord();
            overdueRecord.setId(1L);
            overdueRecord.setMember(member);
            overdueRecord.setBook(book);
            overdueRecord.setDueDate(LocalDate.now().minusDays(3));
            overdueRecord.setIsArchived(false);
            overdueRecord.setLateFee(BigDecimal.ZERO);

            when(borrowRecordRepository.findByStatusAndDueDateBefore(BorrowRecord.BorrowStatus.ACTIVE, LocalDate.now()))
                    .thenReturn(List.of(overdueRecord));

            borrowRecordService.markOverdueRecords();

            assertEquals(BorrowRecord.BorrowStatus.OVERDUE, overdueRecord.getStatus());
            assertEquals(BigDecimal.valueOf(3), overdueRecord.getLateFee());

            verify(emailService).sendOverdueReminder(overdueRecord);
            verify(borrowRecordRepository).saveAll(List.of(overdueRecord));
        }

        @Test
        @DisplayName("Does nothing when no active overdue records exist")
        void doesNothingWhenNoOverdueRecords(){
            when(borrowRecordRepository.findByStatusAndDueDateBefore(BorrowRecord.BorrowStatus.ACTIVE, LocalDate.now()))
                    .thenReturn(List.of());

            borrowRecordService.markOverdueRecords();

            verify(emailService, never()).sendOverdueReminder(any());
            verify(borrowRecordRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Processes multiple overdue records in a single scheduler run")
        void processesMultipleOverdueRecords(){
                BorrowRecord r1 = new BorrowRecord();
                r1.setStatus(BorrowRecord.BorrowStatus.ACTIVE);
                r1.setDueDate(LocalDate.now().minusDays(1));
                r1.setIsArchived(false);
                r1.setLateFee(BigDecimal.ZERO);

                BorrowRecord r2 = new BorrowRecord();
                r2.setStatus(BorrowRecord.BorrowStatus.ACTIVE);
                r2.setDueDate(LocalDate.now().minusDays(5));
                r2.setIsArchived(false);
                r2.setLateFee(BigDecimal.ZERO);

                when(borrowRecordRepository.findByStatusAndDueDateBefore(
                        BorrowRecord.BorrowStatus.ACTIVE, LocalDate.now()))
                        .thenReturn(List.of(r1, r2));

                borrowRecordService.markOverdueRecords();

            assertEquals(BorrowRecord.BorrowStatus.OVERDUE, r1.getStatus());
            assertEquals(BorrowRecord.BorrowStatus.OVERDUE, r2.getStatus());
            verify(emailService, times(2)).sendOverdueReminder(any());
            verify(borrowRecordRepository).saveAll(List.of(r1, r2));
        }
    }

    @Nested
    @DisplayName("updateOverdueRecords()")
    class UpdateOverdueRecords{

        @Test
        @DisplayName("Recalculates late fees and sends reminder emails for all OVERDUE records")
        void updatesLateFeeAndSendsReminders(){
            BorrowRecord overdueRecord = new BorrowRecord();
            overdueRecord.setId(3L);
            overdueRecord.setStatus(BorrowRecord.BorrowStatus.OVERDUE);
            overdueRecord.setDueDate(LocalDate.now().minusDays(10));
            overdueRecord.setIsArchived(false);
            overdueRecord.setLateFee(BigDecimal.valueOf(5)); // stale fee from yesterday

            when(borrowRecordRepository.findByStatus(BorrowRecord.BorrowStatus.OVERDUE))
                    .thenReturn(List.of(overdueRecord));

            borrowRecordService.updateOverdueRecords();

            // Fee recalculated: 10 days × $1 = $10
            assertEquals(BigDecimal.valueOf(10), overdueRecord.getLateFee());
            verify(emailService).sendOverdueReminder(overdueRecord);
            verify(borrowRecordRepository).saveAll(List.of(overdueRecord));
        }

        @Test
        @DisplayName("Caps late fee at $100 regardless of days overdue")
        void capsLateFeeAt100(){
            BorrowRecord veryOverdue = new BorrowRecord();
            veryOverdue.setStatus(BorrowRecord.BorrowStatus.OVERDUE);
            veryOverdue.setDueDate(LocalDate.now().minusDays(200)); // 200 days overdue
            veryOverdue.setIsArchived(false);
            veryOverdue.setLateFee(BigDecimal.ZERO);

            when(borrowRecordRepository.findByStatus(BorrowRecord.BorrowStatus.OVERDUE))
                    .thenReturn(List.of(veryOverdue));

            borrowRecordService.updateOverdueRecords();

            // MAX_LATE_FEE = 100 — never exceeds this
            assertEquals(BigDecimal.valueOf(100), veryOverdue.getLateFee());
        }

        @Test
        @DisplayName("Does nothing when no OVERDUE records exist")
        void doesNothingWhenNoOverdueRecords(){
            when(borrowRecordRepository.findByStatus(BorrowRecord.BorrowStatus.OVERDUE))
                    .thenReturn(List.of());

            borrowRecordService.updateOverdueRecords();

            verify(emailService, never()).sendOverdueReminder(any());
            verify(borrowRecordRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("getMyBorrowRecords()")
    class GetMyBorrowRecords{

        @Test
        @DisplayName("Returns paginated borrow records for the given username")
        void returnsRecordsForUser(){
            Page<BorrowRecord> page = new PageImpl<>(List.of(borrowRecord));

            when(borrowRecordRepository.findAllByMemberName("Alice", PageRequest.of(
                    0, 10,  Sort.by("borrowDate").ascending()
            ))).thenReturn(page);

            when(borrowRecordMapper.toResponse(borrowRecord)).thenReturn(borrowRecordResponse);

            Page<BorrowRecordResponse> result = borrowRecordService.getMyBorrowRecords("Alice", 0, 10, "borrowDate", "ASC");

            assertEquals(1, result.getTotalElements());
            assertEquals("Alice", result.getContent().getFirst().getMember().getName());
        }

        @Test
        @DisplayName("Falls back to 'borrowDate' (not 'id') for invalid sortBy — unique to this method")
        void fallsBackToBorrowDateNotId() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(borrowRecordRepository.findAllByMemberName(eq("alice"), captor.capture()))
                    .thenReturn(Page.empty());

            borrowRecordService.getMyBorrowRecords("alice", 0, 10, "unknown", "ASC");

            // Key difference from getAllBorrowRecords: fallback is 'borrowDate', not 'id'
            assertNotNull(captor.getValue().getSort().getOrderFor("borrowDate"));
            assertNull(captor.getValue().getSort().getOrderFor("id"));
        }

        @Test
        @DisplayName("Caps pageSize at 50")
        void capsPageSizeAt50() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(borrowRecordRepository.findAllByMemberName(eq("Alice"), captor.capture()))
                    .thenReturn(Page.empty());

            borrowRecordService.getMyBorrowRecords("Alice", 0, 200, "borrowDate", "ASC");

            assertEquals(50, captor.getValue().getPageSize());
        }
    }
}