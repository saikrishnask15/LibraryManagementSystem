
package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.common.PageResponse;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberRequest;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.MembershipType;
import com.example.LibraryManagementSystem.service.MemberService;
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
public class MemberController {

    private final MemberService memberService;

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

    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMemberById(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer memberId) {
        return ResponseEntity.ok(memberService.getMemberById(memberId));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(memberService.getMyProfile(userDetails.getUsername()));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @PostMapping
    public ResponseEntity<MemberResponse> addMember(
            @Validated(ValidateGroups.Create.class) @RequestBody MemberRequest request) {
        MemberResponse memberResponse = memberService.addMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponse);
    }

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
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'MEMBER')")
    @PatchMapping("/{memberId}/upgrade")
    public ResponseEntity<MemberResponse> upgradeMembership(@PathVariable Integer memberId,
            @RequestParam MembershipType newTier,
            @AuthenticationPrincipal UserDetails userDetails) {
        MemberResponse memberResponse = memberService.upgradeMembership(memberId, newTier, userDetails.getUsername());
        return ResponseEntity.ok(memberResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer memberId) {
        memberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }

}
