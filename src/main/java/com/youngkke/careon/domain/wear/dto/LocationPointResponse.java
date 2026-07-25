package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.LocationSource;

public record LocationPointResponse(
        Double latitude, Double longitude, Double accuracyMeters, String capturedAt, LocationSource source) {}
