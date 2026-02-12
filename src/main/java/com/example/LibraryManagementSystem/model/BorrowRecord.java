package com.example.LibraryManagementSystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "borrowRecord")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(updatable = false)
    private LocalDate borrowDate;

    private LocalDate dueDate;

    private LocalDateTime returnDate = null;

    @Enumerated(EnumType.STRING)
    private BorrowStatus status = BorrowStatus.ACTIVE;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "archived_by")
    private String archivedBy;

    @Column(name = "archive_reason")
    private String archiveReason;

    public enum BorrowStatus {
        ACTIVE, RETURNED, OVERDUE;
    }

    @PrePersist
    protected void onCreate() {
        if (this.borrowDate == null) {
            this.borrowDate = LocalDate.now();
        }
        if (this.dueDate == null) {
            this.dueDate = this.borrowDate.plusDays(14);
        }
        if (this.status == null) {
            this.status = BorrowStatus.ACTIVE;
        }
        if (isArchived == null) {
            isArchived = false;
        }
    }

}
