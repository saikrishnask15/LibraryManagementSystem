package com.example.LibraryManagementSystem.dto.categoryDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryResponse {

    private Integer id;
    private String name;
    private String description;
    private List<BookDTO> books;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookDTO{
        private Integer id;
        private String title;
    }
}
