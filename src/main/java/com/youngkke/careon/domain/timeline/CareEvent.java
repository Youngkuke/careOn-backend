package com.youngkke.careon.domain.timeline;

import com.youngkke.careon.domain.carer.Cared;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 타임라인에 표시할 사건 1건. 한 번 쓰면 수정하지 않는 append-only 로그다.
 *
 * <p>SOS·이탈은 원본 테이블에서 시각을 뽑아낼 수 있지만, 워치 연결·해제와 위치 공유 시작·종료는 wear_device의
 * 컬럼을 덮어쓰는 구조라 과거 기록이 남지 않는다. 세 테이블을 UNION해서 커서 페이징하는 것보다, 사건이 일어난
 * 자리에서 이 한 테이블에 남기는 편이 정렬과 페이징이 단순하고 정합성도 깨지지 않는다.
 *
 * @param refId 원본 이벤트 id(SOS·이탈). 연결/위치 공유처럼 대응하는 원본 행이 없으면 null이다.
 */
@Entity
@Table(
        name = "care_event",
        indexes = @Index(name = "idx_care_event_cared_occurred_at", columnList = "cared_id, occurred_at DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CareEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_event_id")
    private Integer careEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cared_id", nullable = false)
    private Cared cared;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private CareEventType type;

    /** 사건이 실제로 일어난 시각. 저장 시각이 아니라 원본 이벤트의 발생 시각을 그대로 쓴다. */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "ref_id")
    private Integer refId;

    @Column(name = "summary", nullable = false, length = 200)
    private String summary;
}
