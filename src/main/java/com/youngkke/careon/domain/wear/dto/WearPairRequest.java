package com.youngkke.careon.domain.wear.dto;

import jakarta.validation.constraints.NotBlank;

public record WearPairRequest(@NotBlank(message = "값이 누락되었습니다.") String code) {}
