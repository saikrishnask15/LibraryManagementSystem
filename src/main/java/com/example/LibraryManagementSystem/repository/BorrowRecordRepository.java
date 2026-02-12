package com.example.LibraryManagementSystem.repository;

import com.example.LibraryManagementSystem.model.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {


    Optional<BorrowRecord> findByIdAndIsArchivedFalse(Long id);

    // find all non-archived records
    List<BorrowRecord> findByIsArchivedFalse();

    // find all archived records
    List<BorrowRecord> findByIsArchivedTrue();

    boolean existsByMemberIdAndStatusNot(Integer memberId, BorrowRecord.BorrowStatus borrowStatus);

    boolean existsByBookIdAndStatusNot(Integer bookId, BorrowRecord.BorrowStatus borrowStatus);

    Integer countByMemberIdAndIsArchivedFalse(Integer memberId);
}
