package com.example.LibraryManagementSystem.dto.mapper;

import com.example.LibraryManagementSystem.dto.authorDTO.AuthorRequest;
import com.example.LibraryManagementSystem.dto.authorDTO.AuthorResponse;
import com.example.LibraryManagementSystem.model.Author;

import com.example.LibraryManagementSystem.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthorMapper {

    @Autowired
    private BookRepository bookRepository;

    public Author toEntity(AuthorRequest request){
        return Author.builder()
                .name(request.getName())
                .email(request.getEmail())
                .bio(request.getBio())
                .build();
    }

    public void UpdateEntityFromRequest(Author author, AuthorRequest request){
        if(request.getName() != null){
            author.setName(request.getName());
        }
        if(request.getEmail() != null){
            author.setEmail(request.getEmail());
        }
        if(request.getBio() != null){
            author.setBio(request.getBio());
        }
    }

    public AuthorResponse toResponse(Author author){
        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .email(author.getEmail())
                .bio(author.getBio())
                .bookCount(extractBookCount(author.getId()))
                .build();
    }
    private Integer extractBookCount(Integer id){
        return bookRepository.countByAuthorIdAndAvailableTrue(id);
    }

    public List<AuthorResponse> toResponseList(List<Author> authors){
        return authors.stream()
                .map(this::toResponse)
                .toList();
    }

}
