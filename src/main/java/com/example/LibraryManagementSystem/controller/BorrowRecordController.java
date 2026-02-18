package com.example.LibraryManagementSystem.controller;

import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordRequest;
import com.example.LibraryManagementSystem.dto.borrowRecordDTO.BorrowRecordResponse;
import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import com.example.LibraryManagementSystem.service.BorrowRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/borrowrecords")
@Validated
public class BorrowRecordController {

    @Autowired
    public BorrowRecordService borrowRecordService;

    @GetMapping
    public ResponseEntity<List<BorrowRecordResponse>> getAllRecords(){
        return ResponseEntity.ok(borrowRecordService.getAllBorrowRecords());
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
