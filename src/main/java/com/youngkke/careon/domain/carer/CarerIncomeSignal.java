package com.youngkke.careon.domain.carer;

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

/**
 * 소득 추론 근거(carer_income_signal) 테이블.
 * AI가 사용자의 자연어 발화에서 추출한 소득 관련 근거. 주로 AI 서버가 쓰고, 웹은 조회/충돌해결에 사용한다.
 */
@Entity
@Table(name = "carer_income_signal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CarerIncomeSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "signal_id")
    private Integer signalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carer_id", nullable = false)
    private Carer carer;

    @Column(name = "signal_type", length = 100)
    private String signalType;

    /** 사용자 발화 원문 */
    @Column(name = "raw_value", columnDefinition = "TEXT")
    private String rawValue;

    /** 파싱된 금액 값 */
    @Column(name = "parsed_value")
    private Integer parsedValue;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "confidence", length = 20)
    private String confidence;

    /** 모순되는 다른 시그널의 ID (FK가 아닌 참조값) */
    @Column(name = "contradicts_signal_id")
    private Integer contradictsSignalId;

    @Column(name = "contradiction_resolved")
    private Boolean contradictionResolved;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 충돌 해결: 사용자가 확인한 값을 기록한다. null인 값은 유지. */
    public void resolve(Boolean contradictionResolved, Integer parsedValue) {
        if (contradictionResolved != null) {
            this.contradictionResolved = contradictionResolved;
        }
        if (parsedValue != null) {
            this.parsedValue = parsedValue;
        }
    }
}
