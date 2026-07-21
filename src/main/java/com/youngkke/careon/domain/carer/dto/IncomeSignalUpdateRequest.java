package com.youngkke.careon.domain.carer.dto;

/** 소득 추론 충돌 해결 요청 body. */
public record IncomeSignalUpdateRequest(Boolean contradictionResolved, Integer parsedValue) {}
