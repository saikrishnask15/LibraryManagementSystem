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
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

    public List<BookResponse> getAllBooks() {
        List<Book> books = bookRepository.findAll();
        return bookMapper.toResponseList(books);
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
