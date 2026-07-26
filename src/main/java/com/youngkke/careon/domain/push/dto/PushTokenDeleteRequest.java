package com.youngkke.careon.domain.push.dto;

import jakarta.validation.constraints.NotBlank;

public record PushTokenDeleteRequest(@NotBlank(message = "값이 누락되었습니다.") String token) {}
