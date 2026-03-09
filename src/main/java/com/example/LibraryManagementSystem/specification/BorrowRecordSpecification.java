package com.example.LibraryManagementSystem.specification;

import com.example.LibraryManagementSystem.model.BorrowRecord;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BorrowRecordSpecification {
    public static Specification<BorrowRecord> filterBorrowRecords(
            String memberName,
            String bookName,
            Boolean isArchived,
            String archivedBy,
            BorrowRecord.BorrowStatus status,
            LocalDate borrowedAfter,
            LocalDate borrowedBefore,
            LocalDate dueAfter,
            LocalDate dueBefore,
            BigDecimal minLateFee,
            BigDecimal maxLateFee
    ){
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (memberName != null && !memberName.isEmpty()){
                predicates.add(
                        criteriaBuilder.like(
                                root.join("member").get("name"),
                                "%" + memberName.toLowerCase() + "%"
                        )
                );
            }
            if (bookName != null && !bookName.isEmpty()){
                predicates.add(
                        criteriaBuilder.like(
                                root.join("book").get("title"),
                                "%" + bookName.toLowerCase() + "%"
                        )
                );
            }
            if (isArchived != null){
                predicates.add(
                        criteriaBuilder.equal(root.get("isArchived"), isArchived)
                );
            }
            if(archivedBy != null && !archivedBy.isEmpty()){
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("archivedBy")),
                                "%" + archivedBy.toLowerCase() + "%"
                        )
                );
            }
            if (status != null){
                predicates.add(
                        criteriaBuilder.equal(root.get("status"),status)
                );
            }
            if (borrowedAfter != null){
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("borrowDate"),borrowedAfter)
                );
            }
            if (borrowedBefore != null){
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("borrowDate"),borrowedBefore)
                );
            }
            if (dueAfter != null){
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), dueAfter)
                );
            }
            if (dueBefore != null){
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), dueBefore)
                );
            }
            if (minLateFee != null){
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(root.get("lateFee"), minLateFee)
                );
            }
            if (maxLateFee != null){
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(root.get("lateFee"), maxLateFee)
                );
            }
          return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
