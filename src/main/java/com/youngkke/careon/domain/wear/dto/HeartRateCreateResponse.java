package com.youngkke.careon.domain.wear.dto;

/** 심박수 저장 결과. 서버는 위험도나 진단을 판정하지 않으므로 저장된 사실만 돌려준다. */
public record HeartRateCreateResponse(Integer heartRateId, Integer bpm, String measuredAt) {}
