
package com.example.LibraryManagementSystem.dto.auth;


import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    @NotBlank(message = "Username is Required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

}