package com.youngkke.careon.domain.wear.dto;

public record SafeZoneEventLocationResponse(
        Double latitude, Double longitude, Double accuracyMeters, String capturedAt) {}
