package com.youngkke.careon.domain.wear.dto;

public record SafeZoneResponse(
        Integer safeZoneId, String name, Double latitude, Double longitude, Integer radiusMeters, boolean enabled) {}
