package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.EmergencyStatus;

/** 워치의 확인 상태 polling 응답. */
public record EmergencyEventStatusResponse(Integer eventId, EmergencyStatus status) {}
