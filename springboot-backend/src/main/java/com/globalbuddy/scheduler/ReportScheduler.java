package com.globalbuddy.scheduler;

import com.globalbuddy.model.Report;
import com.globalbuddy.repository.ReportRepository;
import com.globalbuddy.service.ReportModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Report Scheduler
 * Scheduled task to process stuck reports (reports that have been pending for too long)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final ReportRepository reportRepository;
    private final ReportModerationService reportModerationService;

    /**
     * Scheduled task: Execute every 10 minutes
     * Process reports that have been pending for more than 5 minutes
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void processStuckReports() {
        log.info("========== Starting scheduled report processing task ==========");
        
        try {
            // Find reports that have been pending for more than 5 minutes
            Date fiveMinutesAgo = new Date(System.currentTimeMillis() - 5 * 60 * 1000);
            List<Report> stuckReports = reportRepository.findByStatusAndCreatedAtBefore(
                Report.Status.PENDING, 
                fiveMinutesAgo
            );

            if (stuckReports.isEmpty()) {
                log.info("No stuck reports found");
                return;
            }

            log.info("Found {} stuck reports, retrying processing...", stuckReports.size());

            for (Report report : stuckReports) {
                try {
                    log.info("Retrying report processing: id={}, createdAt={}", 
                        report.getId(), report.getCreatedAt());
                    reportModerationService.processReport(report.getId());
                } catch (Exception e) {
                    log.error("Failed to retry report processing: id={}, error={}", 
                        report.getId(), e.getMessage(), e);
                }
            }

            log.info("========== Completed scheduled report processing task ==========");

        } catch (Exception e) {
            log.error("Error in scheduled report processing task: {}", e.getMessage(), e);
        }
    }
}

