package com.youngkke.careon.domain.wear.dto;

public record WearDeviceStatusResponse(
        Integer wearDeviceId, boolean connected, String connectedAt, String lastSeenAt) {}
