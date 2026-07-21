package com.youngkke.careon.domain.document.dto;

import java.util.List;

/** 서류 상세 조회 응답 (발급처 + 이 서류를 요구하는 제도 목록). */
public record DocumentDetailResponse(
        Integer documentId,
        String documentName,
        List<IssuerSummary> issuers,
        List<DocumentPolicyItem> policies) {

    public record DocumentPolicyItem(Integer policyId, String policyName) {}
}
