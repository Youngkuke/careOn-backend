package com.youngkke.careon.domain.wear;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 워치가 측정한 심박수 1건.
 *
 * <p>SOS·이탈처럼 가끔 생기는 이벤트가 아니라 주기적으로 계속 쌓이는 유일한 데이터다. 측정 주기가 1분이면
 * 워치 1대당 하루 1,440행이 들어오므로, 보관 기간과 삭제 정책을 정해 함께 운영해야 한다(아직 미확정).
 * 최신값 조회가 주 용도라 (cared_id, measured_at) 인덱스를 둔다.
 *
 * <p>(wear_device_id, idempotency_key) 유니크 제약으로 재전송에 의한 중복 저장을 DB 레벨에서 막는다.
 */
@Entity
@Table(
        name = "heart_rate",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wear_device_id", "idempotency_key"}),
        indexes = @Index(name = "idx_heart_rate_cared_measured_at", columnList = "cared_id, measured_at DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HeartRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "heart_rate_id")
    private Integer heartRateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cared_id", nullable = false)
    private Cared cared;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wear_device_id", nullable = false)
    private WearDevice wearDevice;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "bpm", nullable = false)
    private Integer bpm;

    /** 센서가 실제로 측정한 시각. 서버가 받은 시각(recordedAt)과 따로 보존한다. */
    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private HeartRateSource source;

    /** 서버 수신 시각. 워치가 오프라인이었다가 몰아서 올리는 경우 measuredAt과 크게 벌어질 수 있다. */
    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;
}
