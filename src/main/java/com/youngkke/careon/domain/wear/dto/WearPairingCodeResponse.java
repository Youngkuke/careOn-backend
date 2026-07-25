package com.youngkke.careon.domain.wear.dto;

/** 워치 연결 코드 발급 응답 (앱). */
public record WearPairingCodeResponse(String code, String expiresAt) {}
