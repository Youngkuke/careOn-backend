package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.SafeZoneResponseType;

public record SafeZoneEventRespondResponse(Integer eventId, SafeZoneResponseType response, String respondedAt) {}
