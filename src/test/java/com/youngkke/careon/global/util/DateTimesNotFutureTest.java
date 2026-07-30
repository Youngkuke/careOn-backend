package com.youngkke.careon.global.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * 워치 시계가 앞으로 밀린 채 값을 올리면 measured_at·detected_at으로 정렬하는 조회가 그 미래 기록에
 * 고정돼, 이후 정상 측정값이 영영 안 보인다. 상한 처리를 회귀 테스트로 고정해둔다.
 */
class DateTimesNotFutureTest {

    @Test
    void 미래_시각은_서버_현재_시각으로_당긴다() {
        String tenHoursAhead = Instant.now().plus(Duration.ofHours(10)).toString();

        LocalDateTime clamped = DateTimes.parseToKstNotFuture(tenHoursAhead);

        assertFalse(clamped.isAfter(LocalDateTime.now(DateTimes.KST)));
    }

    @Test
    void 과거_시각은_그대로_둔다() {
        // 오프라인이었다가 밀린 값을 몰아서 올리는 정상 동작이라 손대면 안 된다.
        String fifteenHoursAgo = Instant.now().minus(Duration.ofHours(15)).toString();

        assertEquals(DateTimes.parseToKst(fifteenHoursAgo), DateTimes.parseToKstNotFuture(fifteenHoursAgo));
    }

    @Test
    void null은_그대로_null이다() {
        assertNull(DateTimes.parseToKstNotFuture(null));
    }
}
