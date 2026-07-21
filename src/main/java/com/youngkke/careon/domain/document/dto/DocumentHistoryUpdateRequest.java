package com.youngkke.careon.domain.document.dto;

/** 서류 이력 수정 요청. 모든 필드 optional. */
public record DocumentHistoryUpdateRequest(
        String issuedDate, String validUntil, Boolean directUtter, Boolean confirmedByUser) {}
