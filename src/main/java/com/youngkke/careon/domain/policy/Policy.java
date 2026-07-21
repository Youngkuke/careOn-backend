package com.youngkke.careon.domain.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** ERD의 "제도(policy)" 테이블. */
@Entity
@Table(name = "policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Integer policyId;

    @Column(name = "policy_name", nullable = false, length = 255)
    private String policyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_type_id", nullable = false)
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PolicyCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Column(name = "support_period", length = 100)
    private String supportPeriod;

    @Column(name = "cost", length = 100)
    private String cost;

    @Column(name = "summary", length = 255)
    private String summary;

    @Column(name = "application_method", length = 255)
    private String applicationMethod;

    @Column(name = "duration", length = 100)
    private String duration;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "application_deadline")
    private LocalDateTime applicationDeadline;

    @Column(name = "result_date")
    private LocalDateTime resultDate;

    @Column(name = "result_note", length = 255)
    private String resultNote;

    @Column(name = "link", length = 500)
    private String link;

    @Column(name = "contact", length = 100)
    private String contact;

    // ===== 이하 신규 컬럼 (아직 로직 미사용, 스키마 매핑용) =====

    @Column(name = "deadline_type", length = 50)
    private String deadlineType;

    @Column(name = "application_region", length = 30)
    private String applicationRegion;

    @Column(name = "schedule_type", length = 20)
    private String scheduleType;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "exception_age", length = 100)
    private String exceptionAge;

    @Column(name = "income_criteria", length = 100)
    private String incomeCriteria;

    @Column(name = "qualification_text", columnDefinition = "TEXT")
    private String qualificationText;

    @Column(name = "support_target", columnDefinition = "TEXT")
    private String supportTarget;

    @Column(name = "duplication_restriction", columnDefinition = "TEXT")
    private String duplicationRestriction;

    @Column(name = "original_notice", columnDefinition = "TEXT")
    private String originalNotice;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "info_reference_year")
    private Integer infoReferenceYear;

    @Column(name = "is_lifetime_limit_once")
    private Boolean isLifetimeLimitOnce;
}
