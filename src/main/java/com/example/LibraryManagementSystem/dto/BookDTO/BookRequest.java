package com.example.LibraryManagementSystem.dto.BookDTO;

import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {

    @NotBlank(message = "Title is required", groups = ValidateGroups.Create.class)
    @Size(min=10, max = 100, message = "Title length must be between 10 to 100",
            groups = {ValidateGroups.Update.class, ValidateGroups.Create.class})
    private String title;

    @NotBlank(message = "ISBN is required",groups = ValidateGroups.Create.class)
    @Size(min = 10, max = 13, message = "ISBN must be valid (10 or 13 digits, with optional hyphens)",
            groups = {ValidateGroups.Update.class, ValidateGroups.Create.class})
    private String isBn;

    @NotNull(message = "publishedYear is required",groups = ValidateGroups.Create.class)
    @Min(value = 1000, message = "Year must be after 1000",
            groups = {ValidateGroups.Update.class, ValidateGroups.Create.class})
    @Max(value = 2026, message = "Year must be before 2026",
            groups = {ValidateGroups.Update.class, ValidateGroups.Create.class})
    private Integer publishedYear;

    private Boolean available = true;

    @NotNull(message = "Total copies is required", groups = ValidateGroups.Create.class)
    @Min(value = 1, message = "Total copies must be at least 1",
            groups = {ValidateGroups.Update.class, ValidateGroups.Create.class})
    private Integer totalCopies;

    @NotNull(message = "Author ID is required", groups = ValidateGroups.Create.class)
    private Integer authorId;

    private List<Integer> categories;

}
