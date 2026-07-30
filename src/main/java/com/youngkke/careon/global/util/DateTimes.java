package com.youngkke.careon.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** API 응답의 날짜/일시 문자열 포맷을 한 곳에서 관리한다. (명세: 일시는 ISO-8601 +09:00, 날짜는 YYYY-MM-DD) */
public final class DateTimes {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private DateTimes() {}

    /** 예: 2026-01-28T00:00:00+09:00 */
    public static String toIsoString(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atOffset(KST_OFFSET).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** 예: 2026-01-28 */
    public static String toDateString(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate().toString();
    }

    public static LocalDate today() {
        return LocalDate.now(KST);
    }

    /** 워치 등 클라이언트가 보낸 ISO-8601 일시(오프셋 불문, 예: ...Z 또는 +09:00)를 KST LocalDateTime으로 변환한다. */
    public static LocalDateTime parseToKst(String isoDateTime) {
        return isoDateTime == null
                ? null
                : OffsetDateTime.parse(isoDateTime).atZoneSameInstant(KST).toLocalDateTime();
    }

    /**
     * 워치가 보낸 일시를 KST로 변환하되, 서버가 받은 시각보다 미래면 받은 시각으로 당긴다.
     *
     * <p>워치 시계가 앞으로 밀린 채 값을 올리면, 그 시각으로 정렬하는 조회들이 미래 기록에 영영 고정된다.
     * (최신 심박수, 진행 중인 SOS·이탈 조회) 워치는 저장 실패를 화면에 띄우지 않으므로 400으로 막으면
     * 기록만 조용히 사라지고 사용자는 성공한 줄 안다. 그래서 값은 살리고 시각만 상한으로 맞춘다.
     *
     * <p>과거 시각은 건드리지 않는다. 워치가 오프라인이었다가 밀린 값을 몰아서 올리는 정상 동작과
     * 시계가 뒤로 밀린 경우를 서버가 구분할 수 없기 때문이다.
     */
    public static LocalDateTime parseToKstNotFuture(String isoDateTime) {
        LocalDateTime parsed = parseToKst(isoDateTime);
        if (parsed == null) {
            return null;
        }
        // 비교 대상이 KST 값이므로 JVM 기본 시간대가 아니라 KST 현재 시각과 견준다.
        LocalDateTime now = LocalDateTime.now(KST);
        return parsed.isAfter(now) ? now : parsed;
    }
}
