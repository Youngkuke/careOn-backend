package com.youngkke.careon.domain.wear.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 워치가 주기적으로 올리는 기기 상태. 이 요청이 들어오는 것 자체가 "아직 살아 있다"는 신호로도 쓰인다. */
public record WearDeviceStatusReportRequest(
        @NotNull(message = "값이 누락되었습니다.")
                @Min(value = 0, message = "값이 올바르지 않습니다.")
                @Max(value = 100, message = "값이 올바르지 않습니다.")
                Integer batteryPercent,
        @NotBlank(message = "값이 누락되었습니다.") String reportedAt) {}
