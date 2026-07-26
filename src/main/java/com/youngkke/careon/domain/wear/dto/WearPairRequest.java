package com.youngkke.careon.domain.wear.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 워치 페어링 요청.
 *
 * @param deviceName 보호자에게 보여줄 기기 표시명. 이미 배포된 워치 앱은 code만 보내므로 선택값으로 두고,
 *     비어 있으면 서버가 기본값을 넣는다. 워치가 보내주기 시작하면 서버 수정 없이 그대로 반영된다.
 */
public record WearPairRequest(
        @NotBlank(message = "값이 누락되었습니다.") String code,
        @Size(max = 100, message = "값이 올바르지 않습니다.") String deviceName) {}
