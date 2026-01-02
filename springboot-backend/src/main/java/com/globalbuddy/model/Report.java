package com.globalbuddy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Report Entity
 * Used to store user reports about posts or comments
 */
@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who submitted the report
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private AppUser reporter;

    /**
     * Type of target: POST or COMMENT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    /**
     * ID of the target (post ID or comment ID)
     */
    @Column(nullable = false)
    private String targetId;

    /**
     * Report reasons (stored as JSON array)
     * Possible values: Spam, Fraud or Scam, Illegal Service Promotion, Abusive Language, Other
     */
    @ElementCollection
    @CollectionTable(name = "report_reasons", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "reason")
    private List<String> reasons;

    /**
     * Optional description text
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Report status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * When the report was created
     */
    @Column(nullable = false)
    @Builder.Default
    private Date createdAt = new Date();

    /**
     * When the report was reviewed
     */
    private Date reviewedAt;

    /**
     * Admin who reviewed the report
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private AppUser reviewedBy;

    /**
     * Review result/notes
     */
    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    public enum TargetType {
        POST,
        COMMENT
    }

    public enum Status {
        PENDING,
        REVIEWED,
        RESOLVED,
        DISMISSED
    }
}

