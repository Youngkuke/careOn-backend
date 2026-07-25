package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.EmergencyStatus;
import com.youngkke.careon.domain.wear.EmergencyTrigger;
import com.youngkke.careon.domain.wear.LocationStatus;

/** 보호자 모바일의 현재 활성(미확인) SOS 조회 응답. */
public record ActiveEmergencyEventResponse(
        Integer eventId,
        Integer caredId,
        EmergencyTrigger trigger,
        Integer heartRateBpm,
        EmergencyStatus status,
        LocationPointResponse location,
        LocationStatus locationStatus,
        String requestedAt) {}
