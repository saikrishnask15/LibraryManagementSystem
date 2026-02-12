package com.example.LibraryManagementSystem.dto.authorDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorWithBooksResponse {
    private Integer id;
    private String name;
    private String email;
    private String bio;
    private List<BookSummary> books;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookSummary{
        private Integer id;
        private String title;
        private String isBn;
        private Integer publishedYear;
        private Boolean available;
    }

}
