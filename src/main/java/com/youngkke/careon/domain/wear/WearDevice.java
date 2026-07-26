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

    /**
     * 보호자가 목록에서 기기를 알아볼 수 있게 하는 표시명. 워치가 페어링 때 보내주면 그 값을,
     * 안 보내주면 기본값을 쓴다. wearDeviceId를 그대로 노출하지 않기 위한 필드다.
     */
    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    /**
     * 보호자가 연결을 해제한 시각. null이면 연결 상태다.
     * row를 지우지 않는 이유는 emergency_event/safe_zone_event가 wear_device_id를 FK로 참조하고 있어서,
     * 삭제하면 이미 쌓인 SOS·이탈 기록이 통째로 깨지기 때문이다. 해제 이력 자체도 타임라인에서 쓴다.
     */
    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;

    @Column(name = "live_location_enabled", nullable = false)
    @Builder.Default
    private boolean liveLocationEnabled = false;

    @Column(name = "live_location_updated_at")
    private LocalDateTime liveLocationUpdatedAt;

    /**
     * 실시간 위치 공유가 자동으로 꺼지는 시각. 보호자가 켜둔 걸 잊어버려도 계속 추적되지 않도록 둔다.
     * 만료됐다고 이 값을 false로 되돌려 쓰지는 않는다. 조회는 읽기 트랜잭션이라 그때마다 쓰기를 만들지 않으려는 것이고,
     * 켜짐 여부는 항상 isLiveLocationActive()로 판단한다.
     */
    @Column(name = "live_location_expires_at")
    private LocalDateTime liveLocationExpiresAt;

    @Column(name = "live_latitude")
    private Double liveLatitude;

    @Column(name = "live_longitude")
    private Double liveLongitude;

    @Column(name = "live_accuracy_meters")
    private Double liveAccuracyMeters;

    @Column(name = "live_captured_at")
    private LocalDateTime liveCapturedAt;

    @Column(name = "battery_percent")
    private Integer batteryPercent;

    @Column(name = "battery_reported_at")
    private LocalDateTime batteryReportedAt;

    /**
     * 배터리 부족 알림을 마지막으로 보낸 시각. 같은 저전력 구간에서 상태를 보고할 때마다 푸시가 반복되는 걸 막는다.
     * 충전으로 배터리가 회복되면 null로 되돌려, 다음에 다시 떨어질 때 알림이 정상적으로 나가게 한다.
     */
    @Column(name = "low_battery_notified_at")
    private LocalDateTime lowBatteryNotifiedAt;

    /** 미통신 알림을 마지막으로 보낸 시각. 워치에서 요청이 다시 들어오면 null로 되돌린다. */
    @Column(name = "offline_notified_at")
    private LocalDateTime offlineNotifiedAt;

    /** 재로그인(refresh) 시 refreshToken 해시를 회전시킨다. */
    public void rotateRefreshToken(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    /**
     * 같은 대상자에 새 연결 코드로 재페어링될 때 호출한다. 기존 row를 지우고 새로 만들면 이미 쌓인
     * emergency_event/safe_zone_event의 wear_device_id FK가 깨지므로, id는 유지한 채 갱신한다.
     * 해제됐던 기기를 다시 연결하는 경우도 여기를 타므로 disconnectedAt을 되돌린다.
     */
    public void reconnect(String deviceName, LocalDateTime connectedAt) {
        this.deviceName = deviceName;
        this.connectedAt = connectedAt;
        this.lastSeenAt = connectedAt;
        this.disconnectedAt = null;
    }

    public boolean isDisconnected() {
        return disconnectedAt != null;
    }

    /**
     * 보호자가 워치 연결을 해제한다. refreshTokenHash를 비워 재발급 경로를 먼저 끊고,
     * 아직 만료되지 않은 access token은 disconnectedAt 검사로 거른다.
     * (워치 access token은 서버 상태를 보지 않는 JWT라, 이 표시가 없으면 남은 유효기간 동안 계속 통과한다.)
     */
    public void disconnect(LocalDateTime now) {
        this.disconnectedAt = now;
        this.refreshTokenHash = "";
        this.liveLocationEnabled = false;
        this.liveLocationExpiresAt = null;
        this.liveLocationUpdatedAt = now;
    }

    /** 워치에서 인증된 요청이 들어올 때마다 호출한다. 연락이 다시 닿았으므로 미통신 알림 표시도 함께 푼다. */
    public void touchLastSeen(LocalDateTime now) {
        this.lastSeenAt = now;
        this.offlineNotifiedAt = null;
    }

    public void reportBattery(Integer batteryPercent, LocalDateTime reportedAt) {
        this.batteryPercent = batteryPercent;
        this.batteryReportedAt = reportedAt;
    }

    public void markLowBatteryNotified(LocalDateTime now) {
        this.lowBatteryNotifiedAt = now;
    }

    /** 충전 등으로 배터리가 회복됐을 때. 다음에 다시 떨어지면 알림이 나가도록 표시를 푼다. */
    public void clearLowBatteryNotified() {
        this.lowBatteryNotifiedAt = null;
    }

    public void markOfflineNotified(LocalDateTime now) {
        this.offlineNotifiedAt = now;
    }

    /** 보호자가 실시간 위치 공유를 켜거나 끈다. 끌 때는 만료 시각도 함께 지운다. */
    public void setLiveLocationEnabled(boolean enabled, LocalDateTime expiresAt, LocalDateTime now) {
        this.liveLocationEnabled = enabled;
        this.liveLocationExpiresAt = enabled ? expiresAt : null;
        this.liveLocationUpdatedAt = now;
    }

    /** 지금 위치를 올려도 되는 상태인지. 켜져 있어도 만료 시각이 지났으면 꺼진 것으로 본다. */
    public boolean isLiveLocationActive(LocalDateTime now) {
        return liveLocationEnabled && liveLocationExpiresAt != null && now.isBefore(liveLocationExpiresAt);
    }

    /** 워치가 보낸 최신 위치로 덮어쓴다 (이력 아님, 최신 1건만 유지). */
    public void updateLiveLocation(Double latitude, Double longitude, Double accuracyMeters, LocalDateTime capturedAt) {
        this.liveLatitude = latitude;
        this.liveLongitude = longitude;
        this.liveAccuracyMeters = accuracyMeters;
        this.liveCapturedAt = capturedAt;
    }
}
