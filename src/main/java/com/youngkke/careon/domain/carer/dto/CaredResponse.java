package com.youngkke.careon.domain.carer.dto;

/** 돌봄 대상자 응답 항목. */
public record CaredResponse(
        Integer caredId, String caredRelation, Integer age, String conditionSummary, String severityLevel) {}
