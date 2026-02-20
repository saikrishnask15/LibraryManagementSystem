package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.BookDTO.BookRequest;
import com.example.LibraryManagementSystem.dto.BookDTO.BookResponse;
import com.example.LibraryManagementSystem.dto.common.PageResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.service.BookService;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Validated
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<PageResponse<BookResponse>> getAllBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String isBn,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Integer minYear,
            @RequestParam(required = false) Integer maxYear,
            @RequestParam(required = false) List<Integer> categoryIds,
            @RequestParam(required = false) Integer minCopies,
            @RequestParam(required = false) Integer maxCopies,
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDir){

        Page<BookResponse> bookResponses = bookService.getAllBooks(
                title, isBn, authorName, available, minYear, maxYear, categoryIds, minCopies, maxCopies, pageNo, pageSize, sortBy, sortDir
        );
        return ResponseEntity.ok(PageResponse.of(bookResponses));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer bookId){
        return ResponseEntity.ok(bookService.getBookById(bookId));
    }

    @PostMapping
    public ResponseEntity<BookResponse> addBook(@Validated(ValidateGroups.Create.class) @RequestBody BookRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.addBook(request));
    }

    @PatchMapping("/{bookId}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable @Min(value = 1, message = "Id must be greater than 0")  Integer bookId,
                                                   @Validated(ValidateGroups.Update.class) @RequestBody BookRequest request){
        return ResponseEntity.ok(bookService.updateBook(bookId, request));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer bookId){
        bookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }
}
