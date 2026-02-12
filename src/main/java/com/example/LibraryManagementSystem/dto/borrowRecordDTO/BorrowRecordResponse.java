package com.example.LibraryManagementSystem.dto.borrowRecordDTO;


import com.example.LibraryManagementSystem.model.BorrowRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BorrowRecordResponse {

    private Long id;

    private MemberDTO member;

    private BookDTO book;

    private LocalDate borrowDate;

    private LocalDate dueDate;

    private LocalDateTime returnDate = null;

    private BorrowRecord.BorrowStatus status;

    private Boolean isArchived;

    private LocalDateTime archivedAt;

    private String archivedBy;

    private String archiveReason;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MemberDTO{
        private Integer id;
        private String name;
        private String email;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BookDTO{
        private Integer id;
        private String title;
    }


}
