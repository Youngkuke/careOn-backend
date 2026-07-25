package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.EmergencyStatus;

public record EmergencyEventAcknowledgeResponse(
        Integer eventId, EmergencyStatus status, String acknowledgedAt, Integer acknowledgedByCarerId) {}
