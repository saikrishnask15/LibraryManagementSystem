
package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.UserResponse;
import com.example.LibraryManagementSystem.dto.common.PageResponse;
import com.example.LibraryManagementSystem.model.Users;
import com.example.LibraryManagementSystem.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Users", description = "Curd operations, filtering, sorting")
public class UsersController {

    private final UsersService usersService;

    @Operation(
            summary = "Get all users",
            description = "Retrieves paginated list of users. Supports sorting and pagination. Requires ADMIN role."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false, defaultValue = "0") int pageNo,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDir

    ) {
        Page<UserResponse> users = usersService.getAllUsers(pageNo, pageSize, sortBy, sortDir);
        return ResponseEntity.ok(PageResponse.of(users));
    }

    @Operation(
            summary = "Get current user details",
            description = "Retrieves detailed information about a current"
    )
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(usersService.getCurrentUser(userDetails.getUsername()));
    }

    @Operation(
            summary = "Delete a User",
            description = "Permanently deletes a User. Requires ADMIN role. " +
                    "Cannot delete if user has active borrows and Admin cannot delete himself."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id, @AuthenticationPrincipal UserDetails userDetails) {
        usersService.deleteUser(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
