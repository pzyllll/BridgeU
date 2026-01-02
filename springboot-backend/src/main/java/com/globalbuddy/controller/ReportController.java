package com.globalbuddy.controller;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.Report;
import com.globalbuddy.repository.ReportRepository;
import com.globalbuddy.service.ContentModerationService;
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
    private final ContentModerationService contentModerationService;

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

            // Trigger AI moderation review for the reported content
            // This will be handled asynchronously or in a separate process
            // For now, we just save the report

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report submitted successfully");
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
    public ResponseEntity<List<Report>> getMyReports() {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Report> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(currentUser.getId());
        return ResponseEntity.ok(reports);
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
}

