package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 페어링된 워치 기기. 돌봄 대상자 1명당 1대만 연결 가능하도록 cared_id에 유니크 제약을 둔다.
 * refreshTokenHash는 평문을 저장하지 않고 SHA-256 해시로 저장한다 (개인정보 최소화 방침).
 * 실시간 위치 공유는 이력이 아니라 최신 위치 1건만 이 row에 덮어써서 보관한다.
 */
@Entity
@Table(name = "wear_device")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WearDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wear_device_id")
    private Integer wearDeviceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cared_id", nullable = false, unique = true)
    private Cared cared;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "live_location_enabled", nullable = false)
    @Builder.Default
    private boolean liveLocationEnabled = false;

    @Column(name = "live_location_updated_at")
    private LocalDateTime liveLocationUpdatedAt;

    @Column(name = "live_latitude")
    private Double liveLatitude;

    @Column(name = "live_longitude")
    private Double liveLongitude;

    @Column(name = "live_accuracy_meters")
    private Double liveAccuracyMeters;

    @Column(name = "live_captured_at")
    private LocalDateTime liveCapturedAt;

    /** 재로그인(refresh) 시 refreshToken 해시를 회전시킨다. */
    public void rotateRefreshToken(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    /**
     * 같은 대상자에 새 연결 코드로 재페어링될 때 호출한다. 기존 row를 지우고 새로 만들면 이미 쌓인
     * emergency_event/safe_zone_event의 wear_device_id FK가 깨지므로, id는 유지한 채 갱신한다.
     */
    public void reconnect(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
        this.lastSeenAt = connectedAt;
    }

    public void touchLastSeen(LocalDateTime now) {
        this.lastSeenAt = now;
    }

    /** 보호자가 실시간 위치 공유를 켜거나 끈다. */
    public void setLiveLocationEnabled(boolean enabled, LocalDateTime now) {
        this.liveLocationEnabled = enabled;
        this.liveLocationUpdatedAt = now;
    }

    /** 워치가 보낸 최신 위치로 덮어쓴다 (이력 아님, 최신 1건만 유지). */
    public void updateLiveLocation(Double latitude, Double longitude, Double accuracyMeters, LocalDateTime capturedAt) {
        this.liveLatitude = latitude;
        this.liveLongitude = longitude;
        this.liveAccuracyMeters = accuracyMeters;
        this.liveCapturedAt = capturedAt;
    }
}
