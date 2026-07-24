package com.youngkke.careon.domain.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;
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

    /** 외부 데이터 파이프라인(AI 서버) 참조용 식별자. 백엔드 API 응답에는 노출하지 않는다. */
    @Column(name = "external_ref", length = 255)
    private String externalRef;

    @Column(name = "policy_name", nullable = false, length = 255)
    private String policyName;

    /*
     * 제도 유형은 다대다 구조로 변경되어 connect_policy_policy_types 테이블(ConnectPolicyPolicyType)로 연결된다.
     * (기존 policy_type_id 단일 FK 컬럼은 DB에서 제거됨)
     */

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
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

    @Column(name = "deadline_type", length = 50)
    private String deadlineType;

    /** 마감일 원문 텍스트 (예: "2025.9.12"). AI 데이터 파이프라인이 채운다. */
    @Column(name = "deadline_date_raw", columnDefinition = "TEXT")
    private String deadlineDateRaw;

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

    /**
     * 신청 기간 유형. FIXED/ALWAYS_OPEN/UNKNOWN.
     * 데이터 파이프라인이 아직 이 값을 채우지 않으면 null이며, 이 경우 getApplicationPeriodTypeOrDefault()가
     * application_deadline 존재 여부로 최선 추정치를 반환한다 (deadline 있으면 FIXED, 없으면 UNKNOWN).
     * "상시 신청 확정"은 데이터 파이프라인/운영이 ALWAYS_OPEN을 명시적으로 채워야 확정된다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "application_period_type", length = 20)
    private ApplicationPeriodType applicationPeriodType;

    /** application_period_type이 비어 있을 때 마감일 존재 여부로 최선 추정치를 계산한다. */
    @Transient
    public ApplicationPeriodType getApplicationPeriodTypeOrDefault() {
        if (applicationPeriodType != null) {
            return applicationPeriodType;
        }
        return applicationDeadline != null ? ApplicationPeriodType.FIXED : ApplicationPeriodType.UNKNOWN;
    }
}
