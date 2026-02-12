package com.example.LibraryManagementSystem.dto.memberDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
class MemberWithBorrowsResponse {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private LocalDate membershipDate;
    private java.util.List<BorrowSummary> borrowRecords;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BorrowSummary {
        private Integer id;
        private String bookTitle;
        private LocalDate borrowDate;
        private LocalDate dueDate;
        private LocalDate returnDate;
        private String status;
    }
}
