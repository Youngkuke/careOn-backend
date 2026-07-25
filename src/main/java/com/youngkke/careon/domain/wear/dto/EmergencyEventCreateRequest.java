package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.EmergencyTrigger;
import com.youngkke.careon.domain.wear.LocationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EmergencyEventCreateRequest(
        @NotNull(message = "값이 누락되었습니다.") EmergencyTrigger trigger,
        Integer heartRateBpm,
        @NotNull(message = "값이 누락되었습니다.") String requestedAt,
        @Valid EmergencyLocationRequest location,
        @NotNull(message = "값이 누락되었습니다.") LocationStatus locationStatus) {}
