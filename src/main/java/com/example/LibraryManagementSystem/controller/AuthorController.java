package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.authorDTO.AuthorRequest;
import com.example.LibraryManagementSystem.dto.authorDTO.AuthorResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.service.AuthorService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authors")
@RequiredArgsConstructor
@Validated
public class AuthorController {

    //lombok constructor injection
    private final AuthorService authorService;

    @GetMapping
    public ResponseEntity<List<AuthorResponse>> getAllAuthors(){
        return ResponseEntity.ok(authorService.getAllAuthors());
    }

    @GetMapping("/{authorId}")
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer authorId){
        return  ResponseEntity.ok(authorService.getAuthorById(authorId));
    }

    @PostMapping
    public ResponseEntity<AuthorResponse> addAuthor(@Validated(ValidateGroups.Create.class) @RequestBody AuthorRequest request){
        AuthorResponse authorRes = authorService.addAuthor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(authorRes);
    }

    @PatchMapping("/{authorId}")
    public ResponseEntity<AuthorResponse> updateAuthor(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer authorId,
                                               @Validated(ValidateGroups.Update.class) @RequestBody AuthorRequest request){
        AuthorResponse author = authorService.updateAuthor(authorId,request);
        return ResponseEntity.ok(author);
    }

    @DeleteMapping("/{authorId}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Integer authorId){
         authorService.deleteAuthor(authorId);
         return ResponseEntity.noContent().build();
    }

}
