package com.youngkke.careon.domain.wear.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SafeZoneEventLocationRequest(
        @NotNull(message = "값이 누락되었습니다.") @DecimalMin(value = "-90", message = "값이 올바르지 않습니다.")
                @DecimalMax(value = "90", message = "값이 올바르지 않습니다.")
                Double latitude,
        @NotNull(message = "값이 누락되었습니다.") @DecimalMin(value = "-180", message = "값이 올바르지 않습니다.")
                @DecimalMax(value = "180", message = "값이 올바르지 않습니다.")
                Double longitude,
        Double accuracyMeters,
        @NotNull(message = "값이 누락되었습니다.") String capturedAt) {}
