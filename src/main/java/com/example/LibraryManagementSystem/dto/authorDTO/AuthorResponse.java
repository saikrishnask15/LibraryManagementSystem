package com.example.LibraryManagementSystem.dto.authorDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorResponse {
    private Integer id;
    private String name;
    private String email;
    private String bio;
    private Integer bookCount;
}
