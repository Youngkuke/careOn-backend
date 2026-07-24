package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ERD의 "저장한 제도(saved_policy)" 테이블.
 * (user_id, policy_id) 유니크 제약으로, 동시에 두 번 저장 요청이 와도 DB 레벨에서 중복 저장을 막는다.
 */
@Entity
@Table(
        name = "saved_policies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"carer_id", "policy_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SavedPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "saved_policy_id")
    private Integer savedPolicyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carer_id", nullable = false)
    private Carer carer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    /**
     * 신청 상태. PREPARING(기본값): 아직 "신청했어요"로 응답하지 않은 상태(서류 준비 중).
     * APPLIED: 유저가 "신청했어요(예)"라고 응답한 상태. ("아니오"는 저장 자체를 삭제하므로 별도 상태값 없음)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", length = 20)
    @Builder.Default
    private ApplicationStatus applicationStatus = ApplicationStatus.PREPARING;

    /** 레거시 데이터 등으로 컬럼 값이 비어 있는 경우를 대비해 PREPARING을 기본값으로 반환한다. */
    public ApplicationStatus getApplicationStatus() {
        return applicationStatus != null ? applicationStatus : ApplicationStatus.PREPARING;
    }

    /** 신청 완료로 기록된 시각. applicationStatus가 APPLIED로 바뀔 때 함께 기록. */
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    /**
     * 수혜 여부. PENDING(기본값): 아직 결과를 모름.
     * 웹에서 신청 완료한 제도에 대해 유저가 직접 RECEIVED/NOT_RECEIVED로 입력.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_status", length = 20)
    @Builder.Default
    private BenefitStatus benefitStatus = BenefitStatus.PENDING;

    /** 레거시 데이터 등으로 컬럼 값이 비어 있는 경우를 대비해 PENDING을 기본값으로 반환한다. */
    public BenefitStatus getBenefitStatus() {
        return benefitStatus != null ? benefitStatus : BenefitStatus.PENDING;
    }

    /** 수혜 여부를 마지막으로 확인/수정한 시각. */
    @Column(name = "benefit_checked_at")
    private LocalDateTime benefitCheckedAt;

    /** 신청 여부. true: application_status == APPLIED. (API 응답의 is_applied 계산용) */
    public boolean isApplied() {
        return getApplicationStatus() == ApplicationStatus.APPLIED;
    }

    /** 마감 지난 제도 또는 상시 신청 제도에 대해 "신청했어요"로 기록한다. */
    public void markApplied() {
        this.applicationStatus = ApplicationStatus.APPLIED;
        this.appliedAt = LocalDateTime.now();
    }

    /** 웹에서 수혜 여부를 기록/수정한다. */
    public void updateBenefitStatus(BenefitStatus benefitStatus) {
        this.benefitStatus = benefitStatus;
        this.benefitCheckedAt = LocalDateTime.now();
    }
}
