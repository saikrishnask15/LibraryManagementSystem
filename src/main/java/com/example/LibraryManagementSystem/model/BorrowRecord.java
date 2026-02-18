package com.example.LibraryManagementSystem.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
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

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal lateFee = BigDecimal.ZERO;

    private LocalDateTime returnDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
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
        if (this.lateFee == null) {
            this.lateFee = BigDecimal.ZERO;
        }
    }

    // Marks record as OVERDUE and calculates late fee
    public void markOverDue(){
        this.status = BorrowStatus.OVERDUE;
        this.lateFee = calculateLateFee();
    }

    public BigDecimal calculateLateFee() {

        if (dueDate == null) return BigDecimal.ZERO;

        if (this.status == BorrowStatus.RETURNED) {
            return this.lateFee;
        }

        if (dueDate.isBefore(LocalDate.now())) {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS
                    .between(dueDate, LocalDate.now());

            return BigDecimal.valueOf(daysOverdue); // 1 per day
        }
        return BigDecimal.ZERO;
    }


}
