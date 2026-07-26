package com.youngkke.careon.domain.wear.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 실시간 위치 공유 시작/중지 요청.
 *
 * @param expiresInMinutes 몇 분 뒤 자동 종료할지. enabled=false면 무시한다. 이미 배포된 앱은 enabled만 보내므로
 *     선택값으로 두고, 비어 있으면 서버 기본값을 쓴다. 허용 범위는 서버가 고정한다.
 */
public record LiveLocationTrackingRequest(
        @NotNull(message = "값이 누락되었습니다.") Boolean enabled,
        @Min(value = 5, message = "값이 올바르지 않습니다.")
                @Max(value = 180, message = "값이 올바르지 않습니다.")
                Integer expiresInMinutes) {}
