package com.youngkke.careon.domain.document.dto;

import jakarta.validation.constraints.NotNull;

/** 서류 이력 저장 요청. */
public record DocumentHistoryCreateRequest(
        @NotNull(message = "값이 누락되었습니다.") Integer documentId,
        @NotNull(message = "값이 누락되었습니다.") Integer policyId,
        String issuedDate,
        String validUntil,
        Boolean directUtter,
        Boolean confirmedByUser) {}
