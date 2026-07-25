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
}
