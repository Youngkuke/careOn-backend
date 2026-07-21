package com.youngkke.careon.domain.carer.dto;

/** 소득 추론 근거 응답 항목. */
public record IncomeSignalResponse(
        Integer signalId,
        String signalType,
        String rawValue,
        Integer parsedValue,
        String source,
        String confidence,
        Integer contradictsSignalId,
        Boolean contradictionResolved,
        String createdAt) {}
