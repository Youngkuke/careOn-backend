package com.youngkke.careon.domain.push.dto;

import com.youngkke.careon.global.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** platform은 생략 가능하며, 비어 있으면 "expo"로 저장한다. */
public record PushTokenRegisterRequest(
        @NotBlank(message = "값이 누락되었습니다.")
                @Pattern(regexp = ValidationPatterns.EXPO_PUSH_TOKEN, message = "값이 올바르지 않습니다.")
                String token,
        String platform) {}
