package com.youngkke.careon.domain.carer.dto;

/** 소득 추론 충돌 해결 응답. */
public record IncomeSignalUpdateResponse(Integer signalId, Boolean contradictionResolved, String message) {}
