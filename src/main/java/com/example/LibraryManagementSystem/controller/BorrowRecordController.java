
package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordRequest;
import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordResponse;
import com.example.LibraryManagementSystem.dto.common.PageResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.service.BorrowRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/borrowrecords")
@Validated
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "BorrowRecords", description = "Curd operations, filtering, sorting")
public class BorrowRecordController {

    private final BorrowRecordService borrowRecordService;

    @Operation(
            summary = "Get all Borrow records",
            description = "Retrieves paginated list of Borrow records with optional filtering by member name," +
                    "book name, is archived, archived by, status, borrowed after borrowed before, due after due before" +
                    "mini late fee, maxi late fee. Supports sorting and pagination. Requires ADMIN or LIBRARIAN role. "
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @GetMapping
    public ResponseEntity<PageResponse<BorrowRecordResponse>> getAllRecords(
            @RequestParam(required = false) String memberName,
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) Boolean isArchived,
            @RequestParam(required = false) String archivedBy,
            @RequestParam(required = false) BorrowRecord.BorrowStatus status,
            @RequestParam(required = false) LocalDate borrowedAfter,
            @RequestParam(required = false) LocalDate borrowedBefore,
            @RequestParam(required = false) LocalDate dueAfter,
            @RequestParam(required = false) LocalDate dueBefore,
            @RequestParam(required = false) BigDecimal minLateFee,
            @RequestParam(required = false) BigDecimal maxLateFee,
            @RequestParam(required = false, defaultValue = "0") int pageNo,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDir) {
        Page<BorrowRecordResponse> borrowRecordResponsePage = borrowRecordService.getAllBorrowRecords(
                memberName, bookName, isArchived, archivedBy, status, borrowedAfter, borrowedBefore,
                dueAfter, dueBefore, minLateFee, maxLateFee, pageNo, pageSize, sortBy, sortDir);
        return ResponseEntity.ok(PageResponse.of(borrowRecordResponsePage));
    }

    @Operation(
            summary = "Get borrow record by ID",
            description = "Retrieves detailed information about a specific borrow record including its member and book" +
                    "Requires ADMIN or LIBRARIAN role. "
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @GetMapping("/{borrowRecordId}")
    public ResponseEntity<BorrowRecordResponse> getBorrowRecordById(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId) {
        return ResponseEntity.ok(borrowRecordService.getBorrowRecordById(borrowRecordId));
    }

    @Operation(
            summary = "Get all archived borrow records",
            description = "Retrieves archived borrow records" +
                    "Requires ADMIN or LIBRARIAN role. "
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @GetMapping("/archived")
    public ResponseEntity<List<BorrowRecordResponse>> getAllArchivedBorrowRecords() {
        return ResponseEntity.ok(borrowRecordService.getAllArchivedBorrowRecords());
    }

    @Operation(
            summary = "Get all non-archived borrow records",
            description = "Retrieves non-archived borrow records" +
                    "Requires ADMIN or LIBRARIAN role. "
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @GetMapping("/non-archived")
    public ResponseEntity<List<BorrowRecordResponse>> getAllActiveBorrowRecords() {
        return ResponseEntity.ok(borrowRecordService.getAllActiveBorrowRecords());
    }

    @Operation(
            summary = "Get all borrow records of current user",
            description = "Retrieves paginated list of Borrow records. Supports sorting and pagination. "
    )
    @GetMapping("/my-records")
    public ResponseEntity<PageResponse<BorrowRecordResponse>> getMyBorrowRecords(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "0") int pageNo,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDir) {

        Page<BorrowRecordResponse> recordResponse = borrowRecordService.getMyBorrowRecords(
                userDetails.getUsername(), pageNo, pageSize, sortBy, sortDir);
        return ResponseEntity.ok(PageResponse.of(recordResponse));
    }

    @Operation(
            summary = "Add a new borrow record",
            description = "Creates a new borrow record in the system." +
                    " Requires ADMIN or LIBRARIAN role or member himself. "
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'MEMBER')")
    @PostMapping
    public ResponseEntity<BorrowRecordResponse> addBorrowRecord(
            @Validated(ValidateGroups.Create.class) @RequestBody BorrowRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BorrowRecordResponse borrowRecordResponse = borrowRecordService.addBorrowRecord(request,
                userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(borrowRecordResponse);
    }

    @Operation(
            summary = "Update borrow record",
            description = "Partially updates borrow record information. Requires ADMIN or LIBRARIAN role."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @PatchMapping("/{borrowRecordId}")
    public ResponseEntity<BorrowRecordResponse> updateBorrowRecord(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId,
            @Validated(ValidateGroups.Update.class) @RequestBody BorrowRecordRequest borrowRecordRequest) {
        return ResponseEntity.ok(borrowRecordService.updateBorrowRecord(borrowRecordId, borrowRecordRequest));
    }

    // patch is used for partial update
    @Operation(
            summary = "Return borrowed book",
            description = "Return borrowed book. Requires ADMIN or LIBRARIAN role or member himself."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'MEMBER')")
    @PatchMapping("/{borrowRecordId}/return")
    public ResponseEntity<BorrowRecordResponse> processReturn(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(borrowRecordService.processReturn(borrowRecordId, userDetails.getUsername()));
    }

    @Operation(
            summary = "Archive the borrow record",
            description = "Requires ADMIN or LIBRARIAN role. Cannot archive if it is active borrow record."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @PatchMapping("/{borrowRecordId}/archive")
    public ResponseEntity<BorrowRecordResponse> archiveBorrowRecord(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId,
            @Validated(ValidateGroups.Update.class) @RequestBody BorrowRecordRequest borrowRecordRequest) {
        BorrowRecordResponse borrowRecordResponse = borrowRecordService.archiveBorrowRecord(borrowRecordId,
                borrowRecordRequest.getArchivedBy(), borrowRecordRequest.getArchiveReason());
        return ResponseEntity.ok(borrowRecordResponse);
    }

    @Operation(
            summary = "Delete a Borrow record",
            description = "Permanently deletes a Borrow record. Requires ADMIN role. " +
                    "Cannot archive if it is active borrow record."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{borrowRecordId}")
    public ResponseEntity<Void> deleteBorrowRecord(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId) {
        borrowRecordService.deleteBorrowRecord(borrowRecordId);
        return ResponseEntity.noContent().build();
    }

}
