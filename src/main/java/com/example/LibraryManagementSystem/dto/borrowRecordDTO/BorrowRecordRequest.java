package com.example.LibraryManagementSystem.dto.borrowRecordDTO;

import com.example.LibraryManagementSystem.dto.validation.ValidateGroups;
import com.example.LibraryManagementSystem.model.BorrowRecord;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BorrowRecordRequest {

    @NotNull(message = "memberId is required", groups = ValidateGroups.Create.class)
    @Min(value = 1, message = "MemberId should greater than 0",
            groups = {ValidateGroups.Update.class, ValidateGroups.Create.class})
    private Integer memberId;

    @NotNull(message = "BookId is required")
    @Min(value = 1, message = "BookId should greater than 0",
            groups = {ValidateGroups.Update.class, ValidateGroups.Create.class})
    private Integer BookId;

    private BorrowRecord.BorrowStatus status;

    private Boolean isArchived = false;

    private String archivedBy;

    private String archiveReason;

    @PrePersist
    protected  void onCreate(){
        if (isArchived == null){
            isArchived = false;
        }
    }

}
