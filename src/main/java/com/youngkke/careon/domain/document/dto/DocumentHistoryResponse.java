package com.youngkke.careon.domain.document.dto;

/** 서류 이력 조회 응답 항목. */
public record DocumentHistoryResponse(
        Integer historyId,
        Integer carerId,
        Integer documentId,
        String documentName,
        Integer policyId,
        String policyName,
        String issuedDate,
        String validUntil,
        Boolean directUtter,
        Boolean confirmedByUser,
        String createdAt) {}
