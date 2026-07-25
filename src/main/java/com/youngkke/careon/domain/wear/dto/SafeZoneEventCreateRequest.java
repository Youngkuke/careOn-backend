package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.SafeZoneEventStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SafeZoneEventCreateRequest(
        @NotNull(message = "값이 누락되었습니다.") Integer safeZoneId,
        @NotNull(message = "값이 누락되었습니다.") SafeZoneEventStatus status,
        @NotNull(message = "값이 누락되었습니다.") String detectedAt,
        @NotNull(message = "값이 누락되었습니다.") @Valid SafeZoneEventLocationRequest location) {}
