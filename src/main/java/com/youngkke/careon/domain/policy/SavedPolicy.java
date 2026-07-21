package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
     * 신청 여부. 마감이 지난 제도에 대해 유저가 "신청했어요(예)"라고 응답하면 true.
     * null이면 아직 응답하지 않은 상태. ("아니오"는 저장 자체를 삭제하므로 false는 사용하지 않음)
     */
    @Column(name = "applied")
    private Boolean applied;

    /** 마감 지난 제도에 대해 "신청했어요"로 기록한다. */
    public void markApplied() {
        this.applied = true;
    }
}
