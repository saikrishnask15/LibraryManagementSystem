
package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.BookDTO.BookRequest;
import com.example.LibraryManagementSystem.dto.BookDTO.BookResponse;
import com.example.LibraryManagementSystem.dto.common.PageResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.service.BookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Books", description = " CRUD operations, filtering, sorting")
public class BookController {

    private final BookService bookService;

    @Operation(
            summary = "Get all books",
            description = "Retrieves paginated list of books with optional filtering by title, ISBN, " +
                    "author, year, category, availability, and copy count. Supports sorting and pagination."
    )
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
            @RequestParam(required = false, defaultValue = "ASC") String sortDir) {

        Page<BookResponse> bookResponses = bookService.getAllBooks(
                title, isBn, authorName, available, minYear, maxYear, categoryIds, minCopies, maxCopies, pageNo,
                pageSize, sortBy, sortDir);
        return ResponseEntity.ok(PageResponse.of(bookResponses));
    }

    @Operation(
            summary = "Get book by ID",
            description = "Retrieves detailed information about a specific book including its author and categories"
    )
    @GetMapping("/{bookId}")
    public ResponseEntity<BookResponse> getBookById(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer bookId) {
        return ResponseEntity.ok(bookService.getBookById(bookId));
    }

    @Operation(
            summary = "Add a new book",
            description = "Creates a new book in the system. Requires ADMIN or LIBRARIAN role. " +
                    "ISBN must be unique."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @PostMapping
    public ResponseEntity<BookResponse> addBook(
            @Validated(ValidateGroups.Create.class) @RequestBody BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.addBook(request));
    }

    @Operation(
            summary = "Update book details",
            description = "Partially updates book information. Requires ADMIN or LIBRARIAN role."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @PatchMapping("/{bookId}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer bookId,
            @Validated(ValidateGroups.Update.class) @RequestBody BookRequest request) {
        return ResponseEntity.ok(bookService.updateBook(bookId, request));
    }

    @Operation(
            summary = "Delete a book",
            description = "Permanently deletes a book. Requires ADMIN role. " +
                    "Cannot delete if book has active borrows."
    )
    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }
}
