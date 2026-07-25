package com.youngkke.careon.domain.wear.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SafeZoneUpsertRequest(
        @NotBlank(message = "값이 누락되었습니다.") String name,
        @NotNull(message = "값이 누락되었습니다.") @DecimalMin(value = "-90", message = "값이 올바르지 않습니다.")
                @DecimalMax(value = "90", message = "값이 올바르지 않습니다.")
                Double latitude,
        @NotNull(message = "값이 누락되었습니다.") @DecimalMin(value = "-180", message = "값이 올바르지 않습니다.")
                @DecimalMax(value = "180", message = "값이 올바르지 않습니다.")
                Double longitude,
        @NotNull(message = "값이 누락되었습니다.") @Min(value = 100, message = "반경은 최소 100m 이상이어야 합니다.")
                Integer radiusMeters,
        @NotNull(message = "값이 누락되었습니다.") Boolean enabled) {}
