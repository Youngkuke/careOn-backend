package com.youngkke.careon.domain.document;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.policy.Policy;
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

/** 서류 이력(user_document_history) 테이블. 유저가 발급했거나 보유한다고 말한 서류 기록. */
@Entity
@Table(name = "user_document_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserDocumentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carer_id", nullable = false)
    private Carer carer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    /** 발급일 */
    @Column(name = "issued_date")
    private LocalDateTime issuedDate;

    /** 유효기간 또는 사용자 표현 (예: "2026-10-01", "3개월 전") */
    @Column(name = "valid_until", length = 30)
    private String validUntil;

    /** 사용자가 직접 말했는지 여부 */
    @Column(name = "direct_utter")
    private Boolean directUtter;

    /** 사용자 확인 여부 */
    @Column(name = "confirmed_by_user")
    private Boolean confirmedByUser;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 부분 수정: null인 값은 유지하고, 값이 있는 필드만 갱신한다. */
    public void update(LocalDateTime issuedDate, String validUntil, Boolean directUtter, Boolean confirmedByUser) {
        if (issuedDate != null) {
            this.issuedDate = issuedDate;
        }
        if (validUntil != null) {
            this.validUntil = validUntil;
        }
        if (directUtter != null) {
            this.directUtter = directUtter;
        }
        if (confirmedByUser != null) {
            this.confirmedByUser = confirmedByUser;
        }
    }
}
