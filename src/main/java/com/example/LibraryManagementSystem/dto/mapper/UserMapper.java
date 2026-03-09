package com.example.LibraryManagementSystem.dto.mapper;

import com.example.LibraryManagementSystem.dto.UserResponse;
import com.example.LibraryManagementSystem.model.Users;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(Users user){
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
