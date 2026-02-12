package com.example.LibraryManagementSystem.dto.mapper;

import com.example.LibraryManagementSystem.dto.categoryDTO.CategoryRequest;
import com.example.LibraryManagementSystem.dto.categoryDTO.CategoryResponse;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.model.Category;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest categoryRequest){
        return Category.builder()
                .name(categoryRequest.getName())
                .description(categoryRequest.getDescription())
                .build();
    }

    public CategoryResponse toResponse(Category category){
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .books(extractBookDTOs(category.getBooks()))
                .build();
    }

    //helping method
    private List<CategoryResponse.BookDTO> extractBookDTOs(List<Book> books){
        if(books == null || books.isEmpty()){
            return new ArrayList<>();
        }
        return books.stream()
                .map(book -> new CategoryResponse.BookDTO(
                        book.getId(),
                        book.getTitle()
                )).toList();
    }

    public List<CategoryResponse> toResponseList(List<Category> categories){
        return categories.stream()
                .map(this::toResponse)
                .toList();
    }
}
