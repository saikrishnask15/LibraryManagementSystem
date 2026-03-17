
package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.common.PageResponse;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberRequest;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.MembershipType;
import com.example.LibraryManagementSystem.service.MemberService;
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

@RestController
@RequestMapping("/api/members")
@Validated
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Members", description = "Curd operations, filtering, sorting")
public class MemberController {

    private final MemberService memberService;

    @Operation(
            summary = "Get all Members",
            description = "Retrieves paginated list of members with optional filtering by name, email," +
                    "phone, membershipType. Supports sorting and pagination. Requires ADMIN or LIBRARIAN role."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @GetMapping
    public ResponseEntity<PageResponse<MemberResponse>> getAllMembers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) MembershipType membershipType,
            @RequestParam(required = false, defaultValue = "0") int pageNo,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy, // sort by name/id/something
            @RequestParam(required = false, defaultValue = "ASC") String sortDir) { // sort by asc/desc

        Page<MemberResponse> page = memberService.getAllMembers(
                name, email, phone, membershipType, pageNo, pageSize, sortBy, sortDir);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @Operation(
            summary = "Get member by ID",
            description = "Retrieves detailed information about a specific member including" +
                    " membership date, max books allowed, borrow period days, active borrow count. Requires ADMIN or LIBRARIAN role."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMemberById(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer memberId) {
        return ResponseEntity.ok(memberService.getMemberById(memberId));
    }

    @Operation(
            summary = "Get current member details",
            description = "Retrieves detailed information about a current member including" +
                    " membership date, max books allowed, borrow period days, active borrow count."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'MEMBER')")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(memberService.getMyProfile(userDetails.getUsername()));
    }

    @Operation(
            summary = "Add a new Member",
            description = "Creates a new member in the system. Requires ADMIN or LIBRARIAN role. "
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @PostMapping
    public ResponseEntity<MemberResponse> addMember(
            @Validated(ValidateGroups.Create.class) @RequestBody MemberRequest request) {
        MemberResponse memberResponse = memberService.addMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponse);
    }

    @Operation(
            summary = "Update member details",
            description = "Partially updates member information. Requires ADMIN or LIBRARIAN role or Member himself."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN','MEMBER')")
    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer memberId,
            @Validated(ValidateGroups.Update.class) @RequestBody MemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        MemberResponse memberResponse = memberService.updateMember(memberId, request, userDetails.getUsername());
        return ResponseEntity.ok(memberResponse);
    }

    // membership upgrade
    @Operation(
            summary = "Upgrade membership",
            description = "Membership can we upgrade to STANDARD or PREMIUM. " +
                    "Requires ADMIN or LIBRARIAN role or Member himself."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'MEMBER')")
    @PatchMapping("/{memberId}/upgrade")
    public ResponseEntity<MemberResponse> upgradeMembership(@PathVariable Integer memberId,
            @RequestParam MembershipType newTier,
            @AuthenticationPrincipal UserDetails userDetails) {
        MemberResponse memberResponse = memberService.upgradeMembership(memberId, newTier, userDetails.getUsername());
        return ResponseEntity.ok(memberResponse);
    }

    @Operation(
            summary = "Delete a Member",
            description = "Permanently deletes a member. Requires ADMIN role. " +
                    "Cannot delete if member has active borrows."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer memberId) {
        memberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }

}
