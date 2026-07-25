package com.youngkke.careon.domain.wear.dto;

/** 워치가 위치를 조회/전송해도 되는지 확인하는 응답. */
public record LiveLocationTrackingStatusResponse(boolean enabled, int intervalSeconds) {}
