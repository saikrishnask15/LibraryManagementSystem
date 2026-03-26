package com.example.LibraryManagementSystem.dto;

import com.example.LibraryManagementSystem.model.Users;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Integer id;
    private String username;
    private String email;
    private String phone;
    private Users.Role role;
    private Boolean enabled;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
