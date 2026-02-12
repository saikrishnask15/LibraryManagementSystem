package com.example.LibraryManagementSystem.dto.categoryDTO;

import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.Book;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name cannot be null",
                        groups = ValidateGroups.Create.class)
    @Size(min = 5, max = 50, message = "Member name should greater than or equal to 5 and lesser than or equal to 50",
            groups = {ValidateGroups.Create.class, ValidateGroups.Update.class})
    private String name;

    @Size(max = 1000, message = "description must be lesser than 1000 words",
                        groups = {ValidateGroups.Create.class, ValidateGroups.Update.class})
    private String description;

    private List<Integer> bookIds;

}
