package com.youngkke.careon.domain.wear.dto;

import jakarta.validation.constraints.NotNull;

public record LiveLocationTrackingRequest(@NotNull(message = "값이 누락되었습니다.") Boolean enabled) {}
