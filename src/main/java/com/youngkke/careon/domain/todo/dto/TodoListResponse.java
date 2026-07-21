package com.youngkke.careon.domain.todo.dto;

import java.util.List;

/** 투두 목록 조회 응답 항목 (저장한 제도 하나 단위). */
public record TodoListResponse(
        Integer savedPolicyId,
        Integer policyId,
        String policyName,
        String deadline,
        String sourceUrl,
        List<TodoDocumentDetail> documents) {

    public record TodoDocumentDetail(
            Integer todoId, Integer documentId, String name, List<IssuerDetail> issuers, boolean isChecked) {}

    public record IssuerDetail(String issuer, String site) {}
}
