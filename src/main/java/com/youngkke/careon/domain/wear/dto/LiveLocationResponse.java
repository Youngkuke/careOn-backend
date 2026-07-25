package com.youngkke.careon.domain.wear.dto;

public record LiveLocationResponse(
        boolean isTracking, Double latitude, Double longitude, Double accuracyMeters, String capturedAt) {}
