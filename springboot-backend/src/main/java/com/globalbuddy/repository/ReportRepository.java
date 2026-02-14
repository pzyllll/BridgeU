package com.globalbuddy.repository;

import com.globalbuddy.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByTargetTypeAndTargetId(Report.TargetType targetType, String targetId);
    List<Report> findByReporterIdOrderByCreatedAtDesc(String reporterId);
    List<Report> findByStatusOrderByCreatedAtDesc(Report.Status status);
    List<Report> findByStatusAndCreatedAtBefore(Report.Status status, Date createdAt);
}

