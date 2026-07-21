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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 챗봇 매칭 결과(matched_policy) 테이블. AI 서버가 매칭 결과를 저장하고, 웹은 맞춤 지원 제도 목록으로 조회한다.
 * match_group 컬럼은 이 엔티티 배포 시 ddl-auto가 자동 추가한다. (적합 / 확인_불가)
 */
@Entity
@Table(name = "matched_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchedPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matched_policy_id")
    private Integer matchedPolicyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carer_id", nullable = false)
    private Carer carer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    /** 수혜 여부. 유저가 "이미 이용했어요/안 했어요"로 기록. */
    @Column(name = "was_benefited")
    private Boolean wasBenefited;

    /** 매칭 그룹: "적합" 또는 "확인_불가" (AI 서버가 저장). */
    @Column(name = "match_group", length = 20)
    private String matchGroup;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 수혜 여부를 수정한다. */
    public void updateWasBenefited(boolean wasBenefited) {
        this.wasBenefited = wasBenefited;
    }
}
