package com.example.LibraryManagementSystem.dto.memberDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private LocalDate membershipDate;
    private Integer activeBorrowCount;
}
