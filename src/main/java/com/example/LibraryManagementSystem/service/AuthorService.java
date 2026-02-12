package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.authorDTO.AuthorRequest;
import com.example.LibraryManagementSystem.dto.authorDTO.AuthorResponse;
import com.example.LibraryManagementSystem.dto.mapper.AuthorMapper;
import com.example.LibraryManagementSystem.exception.ConflictException;
import com.example.LibraryManagementSystem.exception.ResourceAlreadyExistsException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.Author;
import com.example.LibraryManagementSystem.repository.AuthorRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorMapper authorMapper;

    public List<AuthorResponse> getAllAuthors() {
        return authorMapper.toResponseList(authorRepository.findAll());
    }

    public AuthorResponse getAuthorById(Integer authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(()-> new ResourceNotFoundException("Author","id",authorId));
        return authorMapper.toResponse(author);
    }

    @Transactional
    public AuthorResponse addAuthor(@Valid AuthorRequest request) {
        // Check if email already exists (if email is provided)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            authorRepository.findByEmail(request.getEmail())
                    .ifPresent(a -> {
                        throw new ResourceAlreadyExistsException("Author", "email", request.getEmail());
                    });
        }

        //using mapper to convert DTO to entity
        Author author = authorMapper.toEntity(request);

        Author savedAuthor = authorRepository.save(author);
        return authorMapper.toResponse(savedAuthor);
    }

    @Transactional
    public AuthorResponse updateAuthor(Integer authorId, AuthorRequest request) {
        Author exisitingAuthor = authorRepository.findById(authorId)
                .orElseThrow(()-> new ResourceNotFoundException("Author","id",authorId));

        if (request.getEmail() != null && !request.getEmail().equals(exisitingAuthor.getEmail())) {
            authorRepository.findByEmail(request.getEmail())
                    .ifPresent(a -> {
                        throw new ResourceAlreadyExistsException("Author", "email", request.getEmail());
                    });

        }
        //using mapper to update entity from DTO
        authorMapper.UpdateEntityFromRequest(exisitingAuthor, request);

        Author author = authorRepository.save(exisitingAuthor);
        return authorMapper.toResponse(author);
    }

    @Transactional
    public void deleteAuthor(Integer authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(()->  new ResourceNotFoundException("Author","id",authorId));
        if(!author.getBooks().isEmpty()) {
            throw new ConflictException("Cannot delete author with existing books");
        }
         authorRepository.delete(author);
    }
}
