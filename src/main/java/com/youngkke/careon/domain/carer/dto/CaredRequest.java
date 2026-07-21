package com.youngkke.careon.domain.carer.dto;

/** 돌봄 대상자 추가/수정 요청 body. */
public record CaredRequest(
        String caredRelation, Integer age, String conditionSummary, String severityLevel) {}
