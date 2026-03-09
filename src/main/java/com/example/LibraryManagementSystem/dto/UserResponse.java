package com.example.LibraryManagementSystem.dto;

import com.example.LibraryManagementSystem.model.Users;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Integer id;
    private String username;
    private String email;
    private Users.Role role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    // No password field!
}
