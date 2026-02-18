package com.example.LibraryManagementSystem.dto.memberDTO;

import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.MembershipType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberRequest {

    @NotBlank(message = "Member name required", groups = ValidateGroups.Create.class)
    @Size(min = 5, max = 50, message = "Member name should greater than or equal to 5 and lesser than or equal to 50",
            groups = {ValidateGroups.Create.class, ValidateGroups.Update.class})
    private String name;

    @NotBlank(message = "Email is required", groups = {ValidateGroups.Create.class})
    @Size(min = 1 , message = "Email cannot be Empty", groups = ValidateGroups.Update.class)
    @Email(message = "Email must be valid",
            groups = {ValidateGroups.Create.class, ValidateGroups.Update.class})
    private String email;

    @NotBlank(message = "Phone number is required", groups = {ValidateGroups.Create.class})
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits",
            groups = {ValidateGroups.Create.class, ValidateGroups.Update.class})
    private String phone;

    private MembershipType membershipType;

}
