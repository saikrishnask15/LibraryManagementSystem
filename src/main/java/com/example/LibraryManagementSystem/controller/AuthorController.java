
package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.authorDTO.AuthorRequest;
import com.example.LibraryManagementSystem.dto.authorDTO.AuthorResponse;
import com.example.LibraryManagementSystem.dto.common.PageResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.service.AuthorService;
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
@RequestMapping("/api/authors")
@RequiredArgsConstructor
@Validated
public class AuthorController {

    // lombok constructor injection
    private final AuthorService authorService;

    @GetMapping
    public ResponseEntity<PageResponse<AuthorResponse>> getAllAuthors(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false, defaultValue = "0") int pageNo,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDir) {
        Page<AuthorResponse> responsePage = authorService.getAllAuthors(
                name, email, id, pageNo, pageSize, sortBy, sortDir);
        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    @GetMapping("/{authorId}")
    public ResponseEntity<AuthorResponse> getAuthorById(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer authorId) {
        return ResponseEntity.ok(authorService.getAuthorById(authorId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @PostMapping
    public ResponseEntity<AuthorResponse> addAuthor(
            @Validated(ValidateGroups.Create.class) @RequestBody AuthorRequest request) {
        AuthorResponse authorRes = authorService.addAuthor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(authorRes);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @PatchMapping("/{authorId}")
    public ResponseEntity<AuthorResponse> updateAuthor(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer authorId,
            @Validated(ValidateGroups.Update.class) @RequestBody AuthorRequest request) {
        AuthorResponse author = authorService.updateAuthor(authorId, request);
        return ResponseEntity.ok(author);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{authorId}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Integer authorId) {
        authorService.deleteAuthor(authorId);
        return ResponseEntity.noContent().build();
    }

}
