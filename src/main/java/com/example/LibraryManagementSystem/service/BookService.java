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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Set;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BorrowRecordRepository borrowRecordRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BookMapper bookMapper;

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

        return bookPage.map(bookMapper::toResponse);
    }

    public BookResponse getBookById(Integer bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> new ResourceNotFoundException("Book","id",bookId));
        return bookMapper.toResponse(book);
    }

    @Transactional
    public BookResponse addBook(BookRequest request) {


        if(bookRepository.existsByIsBn(request.getIsBn())) {
            throw new ResourceAlreadyExistsException("Book with this ISBN already exists");
        }

        //handling authors
        Author author = authorRepository.findById(request.getAuthorId())
                .orElseThrow(()-> new ResourceNotFoundException("Author", "id", request.getAuthorId()));

        //using mapper to convert DTO to entity
        Book book = bookMapper.toEntity(request, author);

        //handling categories
        if(request.getCategories() != null && !request.getCategories().isEmpty()){
            List<Category> managedCategories = categoryRepository.findAllById(request.getCategories());

            if(managedCategories.size() != request.getCategories().size()){
                throw new ResourceNotFoundException("One or more categories not found");
            }
            book.setCategories(managedCategories);
        }

        Book savedBook = bookRepository.save(book);
        return bookMapper.toResponse(savedBook);
    }

    @Transactional
    public BookResponse updateBook(Integer bookId, BookRequest request) {

        Book exisitingBook = bookRepository.findById(bookId)
                .orElseThrow(()-> new ResourceNotFoundException("Book","id",bookId));

        if(request.getIsBn() != null){
            //if book ISBN we are going update is same as other book ISBN it throws exception
            if(bookRepository.existsByIsBn(request.getIsBn()) && !request.getIsBn().equals(exisitingBook.getIsBn())) {
                throw new ResourceAlreadyExistsException("Book", "ISBN", request.getIsBn());
            }
            exisitingBook.setIsBn(request.getIsBn());
        }

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
        return bookMapper.toResponse(savedBook);
    }

    @Transactional
    public void deleteBook(Integer bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(()-> new ResourceNotFoundException("Book","id",bookId));

        boolean isCurrentlyBorrowed  = borrowRecordRepository.existsByBookIdAndStatusNot(bookId, BorrowRecord.BorrowStatus.RETURNED);
        if(isCurrentlyBorrowed){
            throw new ActiveBorrowExistsException("Cannot delete: This book is currently borrowed and not yet returned!");
        }
        bookRepository.delete(book);
    }


}
