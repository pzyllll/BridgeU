package com.globalbuddy.controller;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.Report;
import com.globalbuddy.repository.ReportRepository;
import com.globalbuddy.service.ReportModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Report Controller
 * Handles post and comment reporting
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportRepository reportRepository;
    private final ReportModerationService reportModerationService;

    /**
     * Get current authenticated user
     */
    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return (AppUser) auth.getPrincipal();
    }

    /**
     * Submit a report for a post or comment
     * POST /api/reports
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> submitReport(@RequestBody Map<String, Object> request) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String targetTypeStr = (String) request.get("targetType");
            String targetId = (String) request.get("targetId");
            @SuppressWarnings("unchecked")
            List<String> reasons = (List<String>) request.get("reasons");
            String description = (String) request.get("description");

            if (targetTypeStr == null || targetId == null || reasons == null || reasons.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Missing required fields: targetType, targetId, reasons");
                return ResponseEntity.badRequest().body(error);
            }

            Report.TargetType targetType;
            try {
                targetType = Report.TargetType.valueOf(targetTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Invalid targetType. Must be POST or COMMENT");
                return ResponseEntity.badRequest().body(error);
            }

            // Create report
            Report report = Report.builder()
                    .reporter(currentUser)
                    .targetType(targetType)
                    .targetId(targetId)
                    .reasons(reasons)
                    .description(description)
                    .status(Report.Status.PENDING)
                    .createdAt(new Date())
                    .build();

            report = reportRepository.save(report);
            log.info("Report submitted: id={}, targetType={}, targetId={}, reasons={}", 
                    report.getId(), targetType, targetId, reasons);

            // Trigger AI moderation review for the reported content asynchronously
            reportModerationService.processReport(report.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report submitted successfully. AI review is in progress.");
            response.put("reportId", report.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to submit report: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to submit report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get reports submitted by current user
     * GET /api/reports/my
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyReports() {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Report> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(currentUser.getId());
            
            // Convert to DTO to avoid lazy loading issues
            List<Map<String, Object>> reportDTOs = reports.stream().map(report -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", report.getId());
                dto.put("targetType", report.getTargetType() != null ? report.getTargetType().name() : null);
                dto.put("targetId", report.getTargetId());
                dto.put("reasons", report.getReasons());
                dto.put("description", report.getDescription());
                dto.put("status", report.getStatus() != null ? report.getStatus().name() : null);
                dto.put("createdAt", report.getCreatedAt());
                dto.put("reviewedAt", report.getReviewedAt());
                dto.put("reviewNotes", report.getReviewNotes());
                dto.put("aiResult", report.getAiResult());
                dto.put("aiConfidence", report.getAiConfidence());
                dto.put("violationSnippet", report.getViolationSnippet());
                dto.put("isViolation", report.getIsViolation());
                
                // Add reporter info (only ID to avoid lazy loading)
                if (report.getReporter() != null) {
                    Map<String, Object> reporterInfo = new HashMap<>();
                    reporterInfo.put("id", report.getReporter().getId());
                    reporterInfo.put("username", report.getReporter().getUsername());
                    reporterInfo.put("displayName", report.getReporter().getDisplayName());
                    dto.put("reporter", reporterInfo);
                }
                
                // Add reviewedBy info (only ID to avoid lazy loading)
                if (report.getReviewedBy() != null) {
                    Map<String, Object> reviewedByInfo = new HashMap<>();
                    reviewedByInfo.put("id", report.getReviewedBy().getId());
                    reviewedByInfo.put("username", report.getReviewedBy().getUsername());
                    reviewedByInfo.put("displayName", report.getReviewedBy().getDisplayName());
                    dto.put("reviewedBy", reviewedByInfo);
                }
                
                return dto;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(reportDTOs);
            
        } catch (Exception e) {
            log.error("Failed to get my reports: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get reports: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all pending reports (admin only)
     * GET /api/reports/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Report>> getPendingReports() {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() != AppUser.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Report> reports = reportRepository.findByStatusOrderByCreatedAtDesc(Report.Status.PENDING);
        return ResponseEntity.ok(reports);
    }

    /**
     * Retry processing a report (admin only)
     * POST /api/reports/{reportId}/retry
     */
    @PostMapping("/{reportId}/retry")
    public ResponseEntity<Map<String, Object>> retryReport(@PathVariable Long reportId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() != AppUser.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

            log.info("Manual retry triggered for report: id={}, currentStatus={}", reportId, report.getStatus());

            // Trigger AI moderation review again
            reportModerationService.processReport(reportId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report processing retried. AI review is in progress.");
            response.put("reportId", reportId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retry report: id={}", reportId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retry report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Retry processing all stuck reports (admin only)
     * POST /api/reports/retry-stuck
     */
    @PostMapping("/retry-stuck")
    public ResponseEntity<Map<String, Object>> retryStuckReports() {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null || currentUser.getRole() != AppUser.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // Find reports that have been pending for more than 5 minutes
            Date fiveMinutesAgo = new Date(System.currentTimeMillis() - 5 * 60 * 1000);
            List<Report> stuckReports = reportRepository.findByStatusAndCreatedAtBefore(
                Report.Status.PENDING, 
                fiveMinutesAgo
            );

            if (stuckReports.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "No stuck reports found.");
                response.put("count", 0);
                return ResponseEntity.ok(response);
            }

            log.info("Manual retry triggered for {} stuck reports", stuckReports.size());

            int successCount = 0;
            int failCount = 0;

            for (Report report : stuckReports) {
                try {
                    reportModerationService.processReport(report.getId());
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to retry report: id={}", report.getId(), e);
                    failCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Retried %d reports. Success: %d, Failed: %d", 
                stuckReports.size(), successCount, failCount));
            response.put("total", stuckReports.size());
            response.put("successCount", successCount);
            response.put("failCount", failCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to retry stuck reports", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retry stuck reports: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

