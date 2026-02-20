package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordRequest;
import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordResponse;
import com.example.LibraryManagementSystem.dto.common.PageResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.service.BorrowRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/borrowrecords")
@Validated
public class BorrowRecordController {

    @Autowired
    public BorrowRecordService borrowRecordService;

    @GetMapping
    public ResponseEntity<PageResponse<BorrowRecordResponse>> getAllRecords(
            @RequestParam(required = false) String memberName,
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) Boolean isArchived,
            @RequestParam(required = false) String archivedBy,
            @RequestParam(required = false) BorrowRecord.BorrowStatus status,
            @RequestParam(required = false) LocalDate borrowedAfter,
            @RequestParam(required = false) LocalDate borrowedBefore,
            @RequestParam(required = false) LocalDate dueAfter,
            @RequestParam(required = false) LocalDate dueBefore,
            @RequestParam(required = false) BigDecimal minLateFee,
            @RequestParam(required = false) BigDecimal maxLateFee,
            @RequestParam(required = false, defaultValue = "0") int pageNo,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDir
    ){
        Page<BorrowRecordResponse> borrowRecordResponsePage = borrowRecordService.getAllBorrowRecords(
                memberName, bookName, isArchived, archivedBy, status, borrowedAfter, borrowedBefore,
                dueAfter, dueBefore, minLateFee, maxLateFee, pageNo, pageSize, sortBy,sortDir);
        return ResponseEntity.ok(PageResponse.of(borrowRecordResponsePage));
    }

    @GetMapping("/{borrowRecordId}")
    public ResponseEntity<BorrowRecordResponse> getBorrowRecordById(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId){
        return ResponseEntity.ok(borrowRecordService.getBorrowRecordById(borrowRecordId));
    }

    @GetMapping("/archived")
    public ResponseEntity<List<BorrowRecordResponse>> getAllArchivedBorrowRecords(){
        return ResponseEntity.ok(borrowRecordService.getAllArchivedBorrowRecords());
    }

    @GetMapping("/non-archived")
    public ResponseEntity<List<BorrowRecordResponse>> getAllActiveBorrowRecords(){
        return ResponseEntity.ok(borrowRecordService.getAllActiveBorrowRecords());
    }

    @PostMapping
    public ResponseEntity<BorrowRecordResponse> addBorrowRecord(@Validated(ValidateGroups.Create.class) @RequestBody BorrowRecordRequest request){
        BorrowRecordResponse borrowRecordResponse = borrowRecordService.addBorrowRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(borrowRecordResponse);
    }

    @PutMapping("/{borrowRecordId}")
    public ResponseEntity<BorrowRecordResponse> updateBorrowRecord(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId,
                                                              @Validated(ValidateGroups.Update.class) @RequestBody BorrowRecordRequest borrowRecordRequest){
        return ResponseEntity.ok(borrowRecordService.updateBorrowRecord(borrowRecordId, borrowRecordRequest));
    }

    //patch is used for partial update
    @PatchMapping("/{borrowRecordId}/return")
    public ResponseEntity<BorrowRecordResponse> processReturn(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId){
        return ResponseEntity.ok(borrowRecordService.processReturn(borrowRecordId));
    }

    @PatchMapping("/{borrowRecordId}/archive")
    public ResponseEntity<BorrowRecordResponse> archiveBorrowRecord(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId,
                                                                    @Validated(ValidateGroups.Update.class) @RequestBody BorrowRecordRequest borrowRecordRequest){
        BorrowRecordResponse borrowRecordResponse = borrowRecordService.archiveBorrowRecord(borrowRecordId, borrowRecordRequest.getArchivedBy(), borrowRecordRequest.getArchiveReason());
        return ResponseEntity.ok(borrowRecordResponse);
    }

    @DeleteMapping("/{borrowRecordId}")
    public ResponseEntity<Void> deleteBorrowRecord(@PathVariable @Min(value = 1, message = "Id must be greater than 0") Long borrowRecordId){
        borrowRecordService.deleteBorrowRecord(borrowRecordId);
        return ResponseEntity.noContent().build();
    }

}
