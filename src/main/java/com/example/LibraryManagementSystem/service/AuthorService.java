package com.example.LibraryManagementSystem.service;

import com.example.LibraryManagementSystem.dto.authorDTO.AuthorRequest;
import com.example.LibraryManagementSystem.dto.authorDTO.AuthorResponse;
import com.example.LibraryManagementSystem.dto.mapper.AuthorMapper;
import com.example.LibraryManagementSystem.exception.ConflictException;
import com.example.LibraryManagementSystem.exception.ResourceAlreadyExistsException;
import com.example.LibraryManagementSystem.exception.ResourceNotFoundException;
import com.example.LibraryManagementSystem.model.Author;
import com.example.LibraryManagementSystem.repository.AuthorRepository;
import com.example.LibraryManagementSystem.repository.BookRepository;
import com.example.LibraryManagementSystem.specification.AuthorSpecification;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;

    private final AuthorMapper authorMapper;

    private final BookRepository bookRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "email");

    public Page<AuthorResponse> getAllAuthors(
            String name,
            String email,
            Integer id,
            int pageNo,
            int pageSize,
            String sortBy,
            String sortDir) {

            if(!ALLOWED_SORT_FIELDS.contains(sortBy)){
                sortBy = "id";
            }

            pageSize = Math.min(pageSize, 50);

            Sort sort = sortDir.equalsIgnoreCase("ASC")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize ,sort);

        Specification<Author> spec = AuthorSpecification.filterAuthor(name, email, id);

        Page<Author> authorpage = authorRepository.findAll(spec, pageable);

        return authorpage.map(authorMapper::toResponse);
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
        authorMapper.updateEntityFromRequest(exisitingAuthor, request);

        Author author = authorRepository.save(exisitingAuthor);
        return authorMapper.toResponse(author);
    }

    @Transactional
    public void deleteAuthor(Integer authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(()->  new ResourceNotFoundException("Author","id",authorId));
        if (author.getBooks() != null && !author.getBooks().isEmpty()) {
            throw new ConflictException("Cannot delete author with existing books");
        }
         authorRepository.delete(author);
    }
}
