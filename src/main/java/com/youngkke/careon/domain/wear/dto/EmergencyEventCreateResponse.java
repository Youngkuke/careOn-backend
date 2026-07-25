package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.EmergencyStatus;

public record EmergencyEventCreateResponse(Integer eventId, EmergencyStatus status, String requestedAt) {}
