package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.Cared;
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
 * 긴급 SOS 이벤트. 위치 컬럼들은 이 SOS 1건에 포함되는 스냅샷이며, 상시 위치 이력이 아니다.
 * (wear_device_id, idempotency_key) 유니크 제약으로 동일 요청의 중복 생성을 DB 레벨에서도 막는다.
 */
@Entity
@Table(
        name = "emergency_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wear_device_id", "idempotency_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class EmergencyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emergency_event_id")
    private Integer emergencyEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cared_id", nullable = false)
    private Cared cared;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wear_device_id", nullable = false)
    private WearDevice wearDevice;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger", nullable = false, length = 30)
    private EmergencyTrigger trigger;

    @Column(name = "heart_rate_bpm")
    private Integer heartRateBpm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EmergencyStatus status = EmergencyStatus.PENDING;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "location_accuracy_m")
    private Double locationAccuracyM;

    @Column(name = "location_captured_at")
    private LocalDateTime locationCapturedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_source", length = 20)
    private LocationSource locationSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_status", nullable = false, length = 20)
    private LocationStatus locationStatus;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private Carer acknowledgedBy;

    public boolean isPending() {
        return status == EmergencyStatus.PENDING;
    }

    public void acknowledge(Carer carer, LocalDateTime now) {
        this.status = EmergencyStatus.ACKNOWLEDGED;
        this.acknowledgedBy = carer;
        this.acknowledgedAt = now;
    }
}
