package com.example.LibraryManagementSystem.dto.mapper;

import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordRequest;
import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordResponse;
import com.example.LibraryManagementSystem.model.Book;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.model.Member;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BorrowRecordMapper {

    public BorrowRecord toEntity(BorrowRecordRequest borrowRecordRequest){
        return BorrowRecord.builder()
                .member(Member.builder().id(borrowRecordRequest.getMemberId()).build())
                .book(Book.builder().id(borrowRecordRequest.getBookId()).build())
                .status(borrowRecordRequest.getStatus())
                .isArchived(borrowRecordRequest.getIsArchived())
                .archivedBy(borrowRecordRequest.getArchivedBy())
                .archiveReason(borrowRecordRequest.getArchiveReason())
                .build();
    }



    public BorrowRecordResponse toResponse(BorrowRecord borrowRecord){
        return BorrowRecordResponse.builder()
                .id(borrowRecord.getId())
                .member(extractMembers(borrowRecord.getMember()))
                .book(extractBooks(borrowRecord.getBook()))
                .borrowDate(borrowRecord.getBorrowDate())
                .dueDate(borrowRecord.getDueDate())
                .returnDate(borrowRecord.getReturnDate())
                .status(borrowRecord.getStatus())
                .isArchived(borrowRecord.getIsArchived())
                .archivedAt(borrowRecord.getArchivedAt())
                .archivedBy(borrowRecord.getArchivedBy())
                .archiveReason(borrowRecord.getArchiveReason())
                .build();
    }
    //helping method
    private BorrowRecordResponse.MemberDTO extractMembers(Member member){
      return new BorrowRecordResponse.MemberDTO(member.getId(),
              member.getName(),
              member.getEmail());
    }

    //helping method

    private BorrowRecordResponse.BookDTO extractBooks(Book book){
        return new BorrowRecordResponse.BookDTO(
                book.getId(),
                book.getTitle()
        );
    }

    public List<BorrowRecordResponse> toResponseList(List<BorrowRecord> borrowRecords){
        return borrowRecords.stream()
                .map(this::toResponse)
                .toList();
    }

}
