package com.example.LibraryManagementSystem.dto.mapper;

import com.example.LibraryManagementSystem.dto.BookDTO.BookRequest;
import com.example.LibraryManagementSystem.dto.BookDTO.BookResponse;
import com.example.LibraryManagementSystem.model.Author;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.model.Category;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BookMapper {

    public Book toEntity(BookRequest request, Author author){
        return Book.builder()
                .title(request.getTitle())
                .isBn(request.getIsBn())
                .publishedYear(request.getPublishedYear())
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .totalCopies(request.getTotalCopies() != null ? request.getTotalCopies() : 1)
                .availableCopies(request.getTotalCopies() != null ? request.getTotalCopies() : 1)
                .author(author)
                .build();
    }

    public void updateEntityFromRequest(Book exisitingBook, BookRequest request, Author author){
        if(request.getTitle() != null){
            exisitingBook.setTitle(request.getTitle());
        }
        if(request.getPublishedYear() != null){
            exisitingBook.setPublishedYear(request.getPublishedYear());
        }

        if(request.getAuthorId() != null){
            exisitingBook.setAuthor(author);
        }
    }


    public BookResponse toResponse(Book book) {
        if (book == null){
            return null;
        }
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .isBn(book.getIsBn())
                .publishedYear(book.getPublishedYear())
                .available(book.getAvailable())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .categories(extractCategoryDTOs(book.getCategories()))
                .build();
    }

    public List<BookResponse> toResponseList(List<Book> books){
        return books.stream()
                .map(this::toResponse)
                .toList();
    }


    //helping method
        private List<BookResponse.CategoryDTO> extractCategoryDTOs(List<Category> categories){
                if(categories == null || categories.isEmpty()){
                    return new ArrayList<>();
                }
                return categories.stream()
                        .map(category -> new BookResponse.CategoryDTO(
                                category.getId(),
                                category.getName()
                        )).toList();
        }

}

