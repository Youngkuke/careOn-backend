package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.push.PushMessage;
import com.youngkke.careon.domain.push.PushSender;
import com.youngkke.careon.domain.wear.dto.WearDeviceStatusReportRequest;
import com.youngkke.careon.domain.wear.dto.WearDeviceStatusReportResponse;
import com.youngkke.careon.global.util.DateTimes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 워치 배터리·미통신 상태를 받아 보호자에게 알린다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WearDeviceStatusService {

    /** 이 값 이하로 떨어지면 보호자에게 알린다. 프론트 요청서에 명시된 기준이다. */
    private static final int LOW_BATTERY_THRESHOLD = 20;

    /**
     * 알림을 다시 보낼 수 있게 되는 회복 기준. 임계값과 같은 20%로 두면 20↔21%를 오갈 때마다 알림이 반복되므로
     * 사이에 여유를 둔다.
     */
    private static final int BATTERY_RECOVERY_THRESHOLD = 30;

    /**
     * 충전 없이 계속 낮게 유지될 때 다시 알리기까지의 최소 간격.
     * 회복 기준만으로는 배터리가 계속 낮은 채로 하루가 지나도 다시 알릴 수 없어서 함께 둔다.
     */
    private static final int LOW_BATTERY_COOLDOWN_HOURS = 6;

    /**
     * 이 시간 이상 워치에서 아무 요청이 없으면 미통신으로 본다.
     * 워치가 상태를 얼마나 자주 보낼지는 아직 계약에 없어서, 잠깐 끊긴 걸 오탐하지 않도록 넉넉하게 잡았다.
     * 워치의 보고 주기가 정해지면 그에 맞춰 줄여야 한다.
     */
    private static final int OFFLINE_THRESHOLD_HOURS = 6;

    private final WearDeviceRepository wearDeviceRepository;
    private final PushSender pushSender;

    /** 워치가 기기 상태를 보고한다. 배터리가 기준 이하로 처음 떨어졌을 때만 보호자에게 알린다. */
    @Transactional
    public WearDeviceStatusReportResponse report(Integer wearDeviceId, WearDeviceStatusReportRequest request) {
        WearDevice wearDevice = wearDeviceRepository.getConnectedOrThrow(wearDeviceId);
        LocalDateTime now = LocalDateTime.now();

        wearDevice.reportBattery(request.batteryPercent(), DateTimes.parseToKst(request.reportedAt()));
        wearDevice.touchLastSeen(now);

        boolean notified = false;
        if (request.batteryPercent() >= BATTERY_RECOVERY_THRESHOLD) {
            wearDevice.clearLowBatteryNotified();
        } else if (shouldNotifyLowBattery(wearDevice, request.batteryPercent(), now)) {
            notifyLowBattery(wearDevice, request.batteryPercent());
            wearDevice.markLowBatteryNotified(now);
            notified = true;
        }

        return new WearDeviceStatusReportResponse(wearDevice.getBatteryPercent(), notified);
    }

    /**
     * 오래 연락이 끊긴 워치를 찾아 보호자에게 한 번 알린다.
     * 워치가 스스로 알려줄 수 없는 상황(전원 꺼짐·통신 두절)이라 서버가 주기적으로 확인해야 한다.
     */
    @Transactional
    public int notifyOfflineDevices() {
        LocalDateTime now = LocalDateTime.now();
        List<WearDevice> offline =
                wearDeviceRepository.findAllByDisconnectedAtIsNullAndOfflineNotifiedAtIsNullAndLastSeenAtBefore(
                        now.minusHours(OFFLINE_THRESHOLD_HOURS));

        offline.forEach(wearDevice -> {
            notifyOffline(wearDevice);
            wearDevice.markOfflineNotified(now);
        });
        return offline.size();
    }

    private boolean shouldNotifyLowBattery(WearDevice wearDevice, int batteryPercent, LocalDateTime now) {
        if (batteryPercent > LOW_BATTERY_THRESHOLD) {
            return false;
        }
        LocalDateTime lastNotified = wearDevice.getLowBatteryNotifiedAt();
        return lastNotified == null || lastNotified.isBefore(now.minusHours(LOW_BATTERY_COOLDOWN_HOURS));
    }

    private void notifyLowBattery(WearDevice wearDevice, int batteryPercent) {
        Cared cared = wearDevice.getCared();
        pushSender.sendAfterCommit(
                cared.getCarer(),
                PushMessage.normal(
                        "워치 배터리가 부족해요",
                        "연결된 워치의 배터리가 " + batteryPercent + "% 남았어요. 충전이 필요해요.",
                        Map.of(
                                "type", "WEAR_LOW_BATTERY",
                                "cared_id", cared.getCaredId(),
                                "url", "/wear-device")));
    }

    private void notifyOffline(WearDevice wearDevice) {
        Cared cared = wearDevice.getCared();
        pushSender.sendAfterCommit(
                cared.getCarer(),
                PushMessage.normal(
                        "워치와 연결이 끊겼어요",
                        "연결된 워치에서 " + OFFLINE_THRESHOLD_HOURS + "시간 넘게 신호가 오지 않았어요.",
                        Map.of(
                                "type", "WEAR_OFFLINE",
                                "cared_id", cared.getCaredId(),
                                "url", "/wear-device")));
    }
}
