package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.memberDTO.MemberRequest;
import com.example.LibraryManagementSystem.dto.memberDTO.MemberResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.Member;
import com.example.LibraryManagementSystem.service.MemberService;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
@Validated
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getAllMembers(){
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMemberById(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer memberId){
        return ResponseEntity.ok(memberService.getMemberById(memberId));
    }

    @PostMapping
    public ResponseEntity<MemberResponse> addMember(@Validated(ValidateGroups.Create.class) @RequestBody MemberRequest request){
        MemberResponse memberResponse = memberService.addMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(memberResponse);
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer memberId, @Validated(ValidateGroups.Update.class) @RequestBody MemberRequest request){
        MemberResponse memberResponse = memberService.updateMember(memberId, request);
        return ResponseEntity.ok(memberResponse);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Integer memberId){
        memberService.deleteMember(memberId);
        return ResponseEntity.noContent().build();
    }

}
