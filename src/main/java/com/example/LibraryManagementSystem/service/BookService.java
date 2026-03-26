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
import com.example.LibraryManagementSystem.specification.BookSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    private final AuthorRepository authorRepository;

    private final BorrowRecordRepository borrowRecordRepository;

    private final CategoryRepository categoryRepository;

    private final BookMapper bookMapper;

    private static  final Set<String> ALLOWED_SORT_FIELDS = Set.of(
           "id", "title", "isBn", "publishedYear", "available", "totalCopies", "availableCopies"
    );

    public Page<BookResponse> getAllBooks(String title,
                                          String isBn,
                                          String authorName,
                                          Boolean available,
                                          Integer minYear,
                                          Integer maxYear,
                                          List<Integer> categoryIds,
                                          Integer minCopies,
                                          Integer maxCopies,
                                          Integer pageNo,
                                          Integer pageSize,
                                          String sortBy,
                                          String sortDir) {
        pageSize = Math.min(pageSize, 50);

        if(!ALLOWED_SORT_FIELDS.contains(sortBy)){
            sortBy = "id";
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Specification<Book> spec = BookSpecification.filterBooks(
                title, isBn, authorName, available, minYear, maxYear, categoryIds, minCopies, maxCopies
        );

        Page<Book> bookPage = bookRepository.findAll(spec, pageable);

        log.debug("Found {} books (Page {}/{})",
                bookPage.getTotalElements(), bookPage.getNumber() + 1, bookPage.getTotalPages());

        return bookPage.map(bookMapper::toResponse);
    }

    public BookResponse getBookById(Integer bookId) {

        log.info("Fetching book by ID: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> {
                    log.warn("Book not found - ID: {}", bookId);
                    return new ResourceNotFoundException("Book","id",bookId);
                });
        return bookMapper.toResponse(book);
    }

    @Transactional
    public BookResponse addBook(BookRequest request) {

        log.info("Adding new book - Title: {}, ISBN: {}", request.getTitle(), request.getIsBn());

        if(bookRepository.existsByIsBn(request.getIsBn())) {
            log.warn("Book creation failed - ISBN already exists: {}", request.getIsBn());
            throw new ResourceAlreadyExistsException("Book with this ISBN already exists");
        }

        //handling authors
        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(()->{
                    log.error("Author not found with ID: {}", request.getAuthorId());
                  return new ResourceNotFoundException("Author", "id", request.getAuthorId());
                });

        //using mapper to convert DTO to entity
        Book book = bookMapper.toEntity(request, author);

        //handling categories
        if(request.getCategories() != null && !request.getCategories().isEmpty()){
            List<Category> managedCategories = categoryRepository.findAllById(request.getCategories());

            if(managedCategories.size() != request.getCategories().size()){
                log.warn("Some categories not found. Requested: {}, Found: {}", request.getCategories().size(), managedCategories.size());
                throw new ResourceNotFoundException("One or more categories not found");
            }
            book.setCategories(managedCategories);
        }

        Book savedBook = bookRepository.save(book);
        log.info("Book added successfully - ID: {}, Title: {}", savedBook.getId(), savedBook.getTitle());
        return bookMapper.toResponse(savedBook);
    }

    @Transactional
    public BookResponse updateBook(Integer bookId, BookRequest request) {

        log.info("Updating book - ID: {}", bookId);

        Book exisitingBook = bookRepository.findById(bookId)
                .orElseThrow(()-> {
                    log.error("Book not found for update - ID: {}", bookId);
                    return new ResourceNotFoundException("Book","id",bookId);
                });

        if(request.getIsBn() != null){
            //if book ISBN we are going update is same as other book ISBN it throws exception
            if(bookRepository.existsByIsBn(request.getIsBn()) && !request.getIsBn().equals(exisitingBook.getIsBn())) {
                log.warn("Book update failed - ISBN already exists: {}", request.getIsBn());
                throw new ResourceAlreadyExistsException("Book", "ISBN", request.getIsBn());
            }
            exisitingBook.setIsBn(request.getIsBn());
        }

        // Store old values for logging
        String oldTitle = exisitingBook.getTitle();
        Integer oldCopies = exisitingBook.getTotalCopies();

        Author author = null;
        if(request.getAuthorId() != null ){
            Integer authorId =  request.getAuthorId();
             author = authorRepository.findById(authorId)
                    .orElseThrow(()-> new ResourceNotFoundException("Author","id",authorId));
        // exisitingBook.setAuthor(author);
        }

        if (request.getTotalCopies() != null && request.getTotalCopies() > exisitingBook.getTotalCopies()){
            int addedCopies = request.getTotalCopies() - exisitingBook.getTotalCopies();
            exisitingBook.setAvailableCopies(exisitingBook.getTotalCopies() + addedCopies);
            exisitingBook.setTotalCopies(request.getTotalCopies());
            exisitingBook.setAvailable(true);
        }

        //using mapper to update convert DTO to entity
        bookMapper.updateEntityFromRequest(exisitingBook, request, author);

        //categories
        if(request.getCategories() != null){
            List<Category> managedCategories = categoryRepository.findAllById(request.getCategories());

            if(managedCategories.size() != request.getCategories().size()){
                throw new ResourceNotFoundException("One or more categories not found");
            }
            exisitingBook.setCategories(managedCategories);
        }
        Book savedBook = bookRepository.save(exisitingBook);
        log.info("Book updated - ID: {}, Old title: '{}' -> new title: '{}', Old copies: {} -> New copies: {}",
               bookId, oldTitle, savedBook.getTitle(), oldCopies, savedBook.getTotalCopies());
        return bookMapper.toResponse(savedBook);
    }

    @Transactional
    public void deleteBook(Integer bookId) {

        log.info("Deleting book - ID: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> {
                    log.error("Book not found for deletion - ID: {}", bookId);
                    return new ResourceNotFoundException("Book","id",bookId);
                });

        boolean isCurrentlyBorrowed  = borrowRecordRepository.existsByBookIdAndStatusNot(bookId, BorrowRecord.BorrowStatus.RETURNED);
        if(isCurrentlyBorrowed){
            log.warn("Delete aborted: Book ID {} has an active/unreturned borrow record.",bookId);
            throw new ActiveBorrowExistsException("Cannot delete: This book is currently borrowed and not yet returned!");
        }
        bookRepository.delete(book);
        log.info("Book deleted - ID: {}, Title: '{}'", bookId, book.getTitle());
    }


}
