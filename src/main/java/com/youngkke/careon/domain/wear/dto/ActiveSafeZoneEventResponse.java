package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.SafeZoneEventStatus;
import com.youngkke.careon.domain.wear.SafeZoneResponseType;

/** 보호자 모바일의 현재 활성(워치 사용자 미응답) 안심 구역 이탈 이벤트 조회 응답. */
public record ActiveSafeZoneEventResponse(
        Integer eventId,
        Integer safeZoneId,
        Integer caredId,
        SafeZoneEventStatus status,
        SafeZoneResponseType response,
        SafeZoneEventLocationResponse location,
        String detectedAt,
        String respondedAt) {}
