package com.youngkke.careon.domain.wear.dto;

/** accepted=false면 추적이 꺼져 있어서 위치가 저장되지 않았다는 뜻. */
public record LiveLocationUpdateResponse(boolean accepted) {}
