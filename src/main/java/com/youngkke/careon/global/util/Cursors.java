package com.youngkke.careon.global.util;

import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 목록 API의 커서를 만들고 읽는다.
 *
 * <p>offset(몇 번째부터)이 아니라 "마지막으로 본 항목의 시각과 id"를 담는 keyset 방식이다. 조회하는 동안
 * 새 이벤트가 앞에 끼어들어도 페이지가 밀려서 같은 항목을 두 번 보거나 건너뛰는 일이 없기 때문이다.
 *
 * <p>시각만으로는 같은 시각에 들어온 두 건의 순서가 흔들리므로 id를 함께 담아 tie-breaker로 쓴다.
 * 클라이언트가 내용을 해석하거나 만들어 쓰지 않도록 Base64로 감싸 opaque 문자열로 노출한다.
 */
public final class Cursors {

    private static final String DELIMITER = ":";

    private Cursors() {}

    /** 커서에서 꺼낸 위치. 이 시각·id "다음" 항목부터 반환한다. */
    public record Position(LocalDateTime timestamp, Integer id) {}

    public static String encode(LocalDateTime timestamp, Integer id) {
        String raw = timestamp.atZone(DateTimes.KST).toInstant().toEpochMilli() + DELIMITER + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** 커서가 비어 있으면 null(첫 페이지). 형식이 깨졌으면 400. */
    public static Position decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = raw.split(DELIMITER);
            if (parts.length != 2) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            return new Position(
                    java.time.Instant.ofEpochMilli(Long.parseLong(parts[0]))
                            .atZone(DateTimes.KST)
                            .toLocalDateTime(),
                    Integer.parseInt(parts[1]));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
