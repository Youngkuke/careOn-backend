package com.youngkke.careon.domain.wear;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 오래 연락이 끊긴 워치를 찾아 보호자에게 알린다.
 *
 * <p>전원이 꺼지거나 통신이 끊긴 워치는 스스로 알려줄 수 없으니, 서버가 마지막 연락 시각을 주기적으로 확인해야 한다.
 */
@Component
@RequiredArgsConstructor
public class WearDeviceOfflineScheduler {

    private static final Logger log = LoggerFactory.getLogger(WearDeviceOfflineScheduler.class);

    private final WearDeviceStatusService wearDeviceStatusService;

    /** 판정 기준이 시간 단위라 분 단위로 자주 볼 필요가 없다. 매시 정각에 한 번 확인한다. */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void notifyOfflineDevices() {
        int notified = wearDeviceStatusService.notifyOfflineDevices();
        if (notified > 0) {
            log.info("미통신 알림을 보낸 워치 {}건.", notified);
        }
    }
}
