package com.example.LibraryManagementSystem.dto.BookDTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookResponse {
    private Integer id;
    private String title;
    private String isBn;
    private Integer publishedYear;
    private Boolean available;
    private List<CategoryDTO> categoryNames;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDTO{
        private Integer id;
        private String name;
    }

}