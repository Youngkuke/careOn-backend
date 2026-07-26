package com.youngkke.careon.domain.wear;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 응답 마감이 지난 안심 구역 이탈을 NO_RESPONSE로 확정한다.
 *
 * <p>응답은 워치에서만 들어오는데, 워치가 꺼지거나 배터리가 나가면 그 API가 영영 안 온다. 그러면 이벤트의
 * response가 계속 null로 남아 며칠 전 이탈이 "활성"으로 조회되고, 이력·타임라인에도 결말이 안 남는다.
 * 조회 시점에 처리하지 않고 배치로 도는 이유는, 앱이나 워치가 조회를 안 하는 동안에도 확정돼야 하기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class SafeZoneEventScheduler {

    private static final Logger log = LoggerFactory.getLogger(SafeZoneEventScheduler.class);

    private final SafeZoneService safeZoneService;

    /** 응답 마감이 30초라, 확정이 눈에 띄게 밀리지 않도록 같은 주기로 확인한다. */
    @Scheduled(fixedDelay = 30_000)
    public void confirmNoResponses() {
        int confirmed = safeZoneService.confirmNoResponses();
        if (confirmed > 0) {
            log.info("무응답으로 확정한 안심 구역 이탈 {}건.", confirmed);
        }
    }
}
