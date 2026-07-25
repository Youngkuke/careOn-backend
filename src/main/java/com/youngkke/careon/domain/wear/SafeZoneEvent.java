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

/** 안심 구역 이탈·응답 기록. (wear_device_id, idempotency_key) 유니크 제약으로 중복 생성을 막는다. */
@Entity
@Table(
        name = "safe_zone_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wear_device_id", "idempotency_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SafeZoneEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "safe_zone_event_id")
    private Integer safeZoneEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safe_zone_id", nullable = false)
    private SafeZone safeZone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cared_id", nullable = false)
    private Cared cared;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wear_device_id", nullable = false)
    private WearDevice wearDevice;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SafeZoneEventStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "response", length = 20)
    private SafeZoneResponseType response;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "accuracy_meters")
    private Double accuracyMeters;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public void respond(SafeZoneResponseType response, LocalDateTime now) {
        this.response = response;
        this.respondedAt = now;
    }
}
