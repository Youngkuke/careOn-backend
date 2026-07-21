package com.youngkke.careon.global.error;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** API 명세서의 에러 응답 포맷 {timestamp, status, error, message, path} 을 그대로 따르는 응답 객체. */
@Getter
public class ErrorResponse {

    private final OffsetDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    private ErrorResponse(HttpStatus status, String message, String path) {
        this.timestamp = OffsetDateTime.now(ZoneOffset.of("+09:00"));
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
        this.path = path;
    }

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(status, message, path);
    }
}
