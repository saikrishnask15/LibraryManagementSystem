package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.BookDTO.BookRequest;
import com.example.LibraryManagementSystem.dto.BookDTO.BookResponse;
import com.example.LibraryManagementSystem.dto.mapper.BookMapper;
import com.example.LibraryManagementSystem.exception.ActiveBorrowExistsException;
import com.example.LibraryManagementSystem.exception.ResourceAlreadyExistsException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.Author;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.model.Category;
import com.example.LibraryManagementSystem.repository.AuthorRepository;
import com.example.LibraryManagementSystem.repository.BookRepository;
import com.example.LibraryManagementSystem.repository.BorrowRecordRepository;
import com.example.LibraryManagementSystem.repository.CategoryRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BorrowRecordRepository borrowRecordRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    private Author author;
    private Book book;
    private BookRequest bookRequest;
    private BookResponse bookResponse;
    private Category category;

    @BeforeEach
    void setup() {
        author = new Author();
        author.setId(1);
        author.setName("J.K. Rowling");
        author.setEmail("jk@example.com");
        author.setBooks(new ArrayList<>());

        category = new Category();
        category.setId(10);
        category.setName("Fantasy");

        // Book entity — new Book() skips @Builder.Default, so set every field explicitly
        book = new Book();
        book.setId(1);
        book.setTitle("Harry Potter");
        book.setIsBn("978-0439708180");
        book.setPublishedYear(1997);
        book.setTotalCopies(5);
        book.setAvailableCopies(5);
        book.setAvailable(true);           // Boolean wrapper, NOT primitive — must be set manually
        book.setAuthor(author);
        book.setCategories(new ArrayList<>(List.of(category)));
        book.setBorrowRecords(new ArrayList<>());

        // DTO in
        bookRequest = new BookRequest();
        bookRequest.setTitle("Harry Potter");
        bookRequest.setIsBn("978-0439708180");
        bookRequest.setPublishedYear(1997);
        bookRequest.setTotalCopies(5);
        bookRequest.setAvailable(true);    // BookRequest.available defaults to true
        bookRequest.setAuthorId(1);
        bookRequest.setCategories(List.of(10));

        // DTO out — mirrors the real BookResponse with nested AuthorDTO and CategoryDTO
        BookResponse.AuthorDTO authorDTO = new BookResponse.AuthorDTO(1, "J.K. Rowling");
        BookResponse.CategoryDTO categoryDTO = new BookResponse.CategoryDTO(10, "Fantasy");

        bookResponse = new BookResponse();
        bookResponse.setId(1);
        bookResponse.setTitle("Harry Potter");
        bookResponse.setIsBn("978-0439708180");
        bookResponse.setPublishedYear(1997);
        bookResponse.setAvailable(true);
        bookResponse.setTotalCopies(5);
        bookResponse.setAvailableCopies(5);
        bookResponse.setAuthor(authorDTO);
        bookResponse.setCategories(List.of(categoryDTO));
    }

    @Nested
    @DisplayName("getAllBooks()")
    class GetAllBooks {

        @Test
        @DisplayName("Returns mapped page when valid params are provided")
        void returnsMappedPage() {
            Page<Book> bookPage = new PageImpl<>(List.of(book));
            when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(bookPage);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            Page<BookResponse> result = bookService.getAllBooks(
                    null, null, null, null, null, null, null, null, null,
                    0, 10, "id", "ASC");

            assertEquals(1, result.getTotalElements());
            assertEquals("Harry Potter", result.getContent().getFirst().getTitle());
        }

        @Test
        @DisplayName("Caps pageSize at 50 regardless of requested value")
        void capsPageSizeAt50() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(bookRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            bookService.getAllBooks(null, null, null, null, null, null, null, null, null,
                    0, 999, "id", "ASC");

            assertEquals(50, captor.getValue().getPageSize());
        }

        @ParameterizedTest
        @DisplayName("Falls back to 'id' for disallowed sortBy values")
        @ValueSource(strings = {"author", "bio", "unknown", ""})
        void fallsBackToIdForInvalidSortField(String invalidSortBy) {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(bookRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            bookService.getAllBooks(null, null, null, null, null, null, null, null, null,
                    0, 10, invalidSortBy, "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor("id"));
        }

        @ParameterizedTest
        @DisplayName("Accepts every allowed sortBy field without falling back")
        @ValueSource(strings = {"id", "title", "isBn", "publishedYear", "available", "totalCopies", "availableCopies"})
        void acceptsAllowedSortFields(String validSortBy) {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(bookRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());

            bookService.getAllBooks(null, null, null, null, null, null, null, null, null,
                    0, 10, validSortBy, "ASC");

            assertNotNull(captor.getValue().getSort().getOrderFor(validSortBy));
        }

        @Test
        @DisplayName("Applies descending sort when sortDir is 'DESC'")
        void appliesDescendingSort(){
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(bookRepository.findAll(any(Specification.class), captor.capture()))
                    .thenReturn(Page.empty());
            bookService.getAllBooks(null, null, null, null, null, null, null, null, null,
                    0, 10, "title", "DESC");

            Sort.Order order = captor.getValue().getSort().getOrderFor("title");
            assertNotNull(order);
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Test
        @DisplayName("Returns empty page when no books match the filter")
        void returnsEmptyPage() {
            when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<BookResponse> result = bookService.getAllBooks(
                    "Unknown Title", null, null, null, null, null, null, null, null,
                    0, 10, "id", "ASC");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getBookById()")
    class GetBookById{
        @Test
        @DisplayName("Returns BookResponse when book exists")
        void returnsResponseWhenFound() {
            when(bookRepository.findById(1)).thenReturn(Optional.of(book));
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            BookResponse result = bookService.getBookById(1);

            assertEquals(1, result.getId());
            assertEquals("Harry Potter", result.getTitle());
            assertEquals(1, result.getAuthor().getId());
            assertEquals("J.K. Rowling", result.getAuthor().getName());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when book is missing")
        void throwsWhenNotFound() {
            when(bookRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,() -> bookService.getBookById(99));

            verify(bookMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("addBook()")
    class AddBook{

        @Test
        @DisplayName("Saves and returns BookResponse on a clean request")
        void savesBookSuccessfully(){

            when(bookRepository.existsByIsBn("978-0439708180"))
                    .thenReturn(false);
            when(authorRepository.findById(1))
                    .thenReturn(Optional.of(author));
            when(bookMapper.toEntity(bookRequest, author)).thenReturn(book);
            when(categoryRepository.findAllById(List.of(10)))
                    .thenReturn(List.of(category));
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            BookResponse result = bookService.addBook(bookRequest);

            assertEquals("Harry Potter", result.getTitle());
            assertEquals("978-0439708180", result.getIsBn());
            assertEquals(1, result.getId());
            assertEquals("J.K. Rowling", result.getAuthor().getName());
            assertEquals(1, result.getCategories().size());
            assertEquals("Fantasy", result.getCategories().getFirst().getName());
            verify(bookRepository, times(1)).save(book);
        }

        @Test
        @DisplayName("Throws ResourceAlreadyExistsException when ISBN is already registered")
        void throwsWhenIsbnAlreadyExists(){
            when(bookRepository.existsByIsBn("978-0439708180")).thenReturn(true);
            assertThrows(ResourceAlreadyExistsException.class, ()->bookService.addBook(bookRequest));

            verify(bookRepository, never()).findById(any());
            verify(bookRepository, never()).save(any());
        }


        @Test
        @DisplayName("Throws ResourceNotFoundException when authorId does not exist")
        void throwsWhenAuthorNotFound() {
            when(bookRepository.existsByIsBn("978-0439708180")).thenReturn(false);
            when(authorRepository.findById(1)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,() -> bookService.addBook(bookRequest));
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when one or more category IDs are invalid")
        void throwsWhenSomeCategoriesNotFound(){

            bookRequest.setCategories(List.of(10, 99));

            when(bookRepository.existsByIsBn("978-0439708180")).thenReturn(false);
            when(authorRepository.findById(1)).thenReturn(Optional.of(author));
            when(bookMapper.toEntity(bookRequest, author)).thenReturn(book);
            when(categoryRepository.findAllById(List.of(10, 99))).thenReturn(List.of(category));

            assertThrows(ResourceNotFoundException.class, ()->bookService.addBook(bookRequest));

            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Skips category lookup and saves when categories is null")
        void savesBookWhenCategoriesNull(){
            bookRequest.setCategories(null);

            when(bookRepository.existsByIsBn("978-0439708180")).thenReturn(false);
            when(authorRepository.findById(1)).thenReturn(Optional.of(author));
            when(bookMapper.toEntity(bookRequest, author)).thenReturn(book);
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            bookService.addBook(bookRequest);


            verify(bookRepository, never()).findAllById(any());
            verify(bookRepository, times(1)).save(book);
        }

        @Test
        @DisplayName("Skips category lookup and saves when categories is an empty list")
        void savesBookWhenCategoriesEmpty() {
            bookRequest.setCategories(List.of()); // isEmpty() guard in service

            when(bookRepository.existsByIsBn("978-0439708180")).thenReturn(false);
            when(authorRepository.findById(1)).thenReturn(Optional.of(author));
            when(bookMapper.toEntity(bookRequest, author)).thenReturn(book);
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            bookService.addBook(bookRequest);

            verify(categoryRepository, never()).findAllById(any());
            verify(bookRepository).save(book);
        }
    }

    @Nested
    @DisplayName("UpdateBook()")
    class UpdateBook{

        @Test
        @DisplayName("Throws ResourceNotFoundException when book to update does not exist")
        void throwsWhenBookNotFound(){

            when(bookRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->bookService.updateBook(99, bookRequest));

            verify(bookRepository, never()).save(any());

        }

        @Test
        @DisplayName("Throws ResourceAlreadyExistsException when new ISBN belongs to a different book")
        void throwsWhenIsbnTakenByAnotherBook(){

            book.setIsBn("978-ORIGINAL");
            bookRequest.setIsBn("978-DUPLICATE");

            when(bookRepository.findById(1)).thenReturn(Optional.of(book));
            when(bookRepository.existsByIsBn("978-DUPLICATE")).thenReturn(true);

            assertThrows(ResourceAlreadyExistsException.class, ()->bookService.updateBook(1, bookRequest));

            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Allows update when ISBN in request is the same as the existing book's own ISBN")
        void allowsUpdateWithSameIsbn(){

            when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
            // existsByIsBn returns true, but it's the same book — equals() guard prevents the throw
            when(bookRepository.existsByIsBn("978-0439708180")).thenReturn(true);
            when(authorRepository.findById(1)).thenReturn(Optional.ofNullable(author));
            when(categoryRepository.findAllById(List.of(10))).thenReturn(List.of(category));
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            BookResponse result = bookService.updateBook(1, bookRequest);

            assertNotNull(result);
            verify(bookRepository, times(1)).save(book);
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when new authorId does not exist")
        void throwsWhenNewAuthorNotFound(){

            bookRequest.setAuthorId(99);
            bookRequest.setIsBn(null);

            when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
            when(authorRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, ()->bookService.updateBook(1, bookRequest));

            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Increases availableCopies proportionally when totalCopies is raised")
        void increasesAvailableCopiesWhenTotalCopiesRaised(){

            book.setTotalCopies(5);
            book.setAvailableCopies(3);
            book.setAvailable(true);

            bookRequest.setTotalCopies(8);
            bookRequest.setIsBn(null);
            bookRequest.setAuthorId(null);
            bookRequest.setCategories(null);

            //set bookResponse to reflect EXPECTED state after update
            bookResponse.setAvailableCopies(6);  // 3 existing + 3 new
            bookResponse.setTotalCopies(8);
            bookResponse.setAvailable(true);

            when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            BookResponse result = bookService.updateBook(1, bookRequest);

            assertEquals(6, result.getAvailableCopies());
            assertEquals(8, result.getTotalCopies());
            assertTrue(result.getAvailable());
        }

        @Test
        @DisplayName("Does not adjust copies when new totalCopies is lower than current")
        void doesNotChangeCopiesWhenTotalNotIncreased(){
            book.setTotalCopies(5);
            book.setAvailableCopies(3);
            book.setAvailable(true);

            bookRequest.setTotalCopies(4);
            bookRequest.setIsBn(null);
            bookRequest.setAuthorId(null);
            bookRequest.setCategories(null);

            when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            BookResponse result = bookService.updateBook(1, bookRequest);

            assertEquals(5, result.getTotalCopies());
        }

        @Test
        @DisplayName("Does not adjust copies when new totalCopies equals current")
        void doesNotChangeCopiesWhenTotalEqualsExisting(){
            book.setTotalCopies(5);
            book.setAvailableCopies(3);
            book.setAvailable(true);

            bookRequest.setTotalCopies(5);
            bookRequest.setIsBn(null);
            bookRequest.setAuthorId(null);
            bookRequest.setCategories(null);

            when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            BookResponse result = bookService.updateBook(1, bookRequest);

            assertEquals(5, result.getTotalCopies());
        }

        @Test
        @DisplayName("Throws ResourceNotFoundException when a category ID in update request is invalid")
        void throwsWhenCategoryNotFoundOnUpdate(){

            bookRequest.setIsBn(null);
            bookRequest.setAuthorId(null);
            bookRequest.setCategories(List.of(10,20));

            when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
            when(categoryRepository.findAllById(List.of(10,20))).thenReturn(List.of(category));

            assertThrows(ResourceNotFoundException.class, ()->bookService.updateBook(1, bookRequest));
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Skips category update when categories in request is null")
        void skipsCategoryUpdateWhenNull(){

            bookRequest.setIsBn(null);
            bookRequest.setCategories(null); //null → service skips the category entire block

            when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
            when(authorRepository.findById(1)).thenReturn(Optional.ofNullable(author));
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            bookService.updateBook(1, bookRequest);

            verify(categoryRepository, never()).findAllById(any());
            verify(bookRepository).save(book);

        }

        @Test
        @DisplayName("Skips author lookup when authorId in request is null")
        void skipsAuthorLookupWhenAuthorIdIsNull(){

            bookRequest.setAuthorId(null);
            bookRequest.setIsBn(null);
            bookRequest.setCategories(null);

            when(bookRepository.findById(1)).thenReturn(Optional.of(book));
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            bookService.updateBook(1, bookRequest);

            verify(authorRepository, never()).findById(any());
            verify(bookRepository).save(book);
        }

        @Test
        @DisplayName("Skips ISBN check when isBn in request is null")
        void skipsIsbnCheckWhenIsbnIsNull(){

            bookRequest.setIsBn(null);
            bookRequest.setAuthorId(null);
            bookRequest.setCategories(null);

            when(bookRepository.findById(1)).thenReturn(Optional.of(book));
            when(bookRepository.save(book)).thenReturn(book);
            when(bookMapper.toResponse(book)).thenReturn(bookResponse);

            bookService.updateBook(1, bookRequest);

            verify(bookRepository, never()).existsByIsBn(any());
            verify(bookRepository).save(book);
        }

        @Nested
        @DisplayName("deleteBook()")
        class DeleteBook{

            @Test
            @DisplayName("Deletes book when it exists and has no active borrow records")
            void deletesSuccessfully(){

                when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
                when(borrowRecordRepository.existsByBookIdAndStatusNot(1,
                        BorrowRecord.BorrowStatus.RETURNED)).thenReturn(false);

                bookService.deleteBook(1);

                verify(bookRepository, times(1)).delete(book);
            }

            @Test
            @DisplayName("Throws ResourceNotFoundException when book to delete does not exist")
            void throwsWhenBookNotFound(){

                when(bookRepository.findById(99)).thenReturn(Optional.empty());

                assertThrows(ResourceNotFoundException.class, ()->bookService.deleteBook(99));

                verify(borrowRecordRepository, never()).existsByBookIdAndStatusNot(1, BorrowRecord.BorrowStatus.RETURNED);
                verify(bookRepository, never()).delete((Book) any());
            }

            @Test
            @DisplayName("Throws ActiveBorrowExistsException when book has an unreturned borrow record")
            void throwsWhenBookIsCurrentlyBorrowed(){

                when(bookRepository.findById(1)).thenReturn(Optional.ofNullable(book));
                when(borrowRecordRepository.existsByBookIdAndStatusNot(1, BorrowRecord.BorrowStatus.RETURNED))
                        .thenReturn(true);

                assertThrows(ActiveBorrowExistsException.class, ()->bookService.deleteBook(1));

                verify(bookRepository, never()).delete((Book) any());
            }

        }

    }

}