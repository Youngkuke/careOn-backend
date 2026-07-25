package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.LocationSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** location이 null이 아닐 때만 검증된다 (Bean Validation은 null 필드에 @Valid를 적용하지 않음). */
public record EmergencyLocationRequest(
        @NotNull(message = "값이 누락되었습니다.") @DecimalMin(value = "-90", message = "값이 올바르지 않습니다.")
                @DecimalMax(value = "90", message = "값이 올바르지 않습니다.")
                Double latitude,
        @NotNull(message = "값이 누락되었습니다.") @DecimalMin(value = "-180", message = "값이 올바르지 않습니다.")
                @DecimalMax(value = "180", message = "값이 올바르지 않습니다.")
                Double longitude,
        Double accuracyMeters,
        @NotNull(message = "값이 누락되었습니다.") String capturedAt,
        @NotNull(message = "값이 누락되었습니다.") LocationSource source) {}
