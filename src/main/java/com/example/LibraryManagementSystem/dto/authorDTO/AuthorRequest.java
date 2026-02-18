package com.example.LibraryManagementSystem.dto.authorDTO;

import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorRequest {

    @NotBlank(groups = ValidateGroups.Create.class, message = "Author name is required")
    @Size(min = 3, max = 50, message = "Author name must be between 3 and 50 characters",
            groups = {ValidateGroups.Create.class, ValidateGroups.Update.class})
    private String name;

    @Email(message = "Email must be valid",
            groups = {ValidateGroups.Create.class, ValidateGroups.Update.class})
    private String email;

    @Size(max = 100, message = "Bio must not exceed 100 characters",
            groups = {ValidateGroups.Create.class, ValidateGroups.Update.class})
    private String bio;
}
