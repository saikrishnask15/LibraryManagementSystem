package com.example.LibraryManagementSystem.scheduler;

import com.example.LibraryManagementSystem.service.BorrowRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OverDueJob {

    private final BorrowRecordService borrowRecordService;

    @Scheduled(cron = "0 0 * * * ?")
    public void runDailyOverdueChecks(){
        borrowRecordService.markOverdueRecords();
    }

    @Scheduled(cron = "0 5 * * * ?")
    public void updateOverdueChecks(){
        borrowRecordService.updateOverdueRecords();
    }


}
