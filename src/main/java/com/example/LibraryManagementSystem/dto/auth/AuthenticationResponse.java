package com.example.LibraryManagementSystem.dto.auth;

import com.example.LibraryManagementSystem.model.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {

    private String token;
    private String username;
    private String email;
    private String phone;
    private Users.Role role;
    private Integer memberId;
    private String message;
}