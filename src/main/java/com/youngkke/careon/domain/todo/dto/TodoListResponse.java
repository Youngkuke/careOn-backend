package com.youngkke.careon.domain.todo.dto;

import com.youngkke.careon.domain.document.dto.IssuerSummary;
import java.util.List;

/** 투두 목록 조회(앱) 응답 항목 (저장한 제도 하나 단위). */
public record TodoListResponse(
        Integer savedPolicyId,
        Integer policyId,
        String policyName,
        String applicationDeadline,
        String applicationPeriodType,
        boolean isExpired,
        String applicationStatus,
        boolean isApplied,
        String appliedAt,
        String resultDate,
        String link,
        List<TodoDocumentDetail> documents) {

    public record TodoDocumentDetail(
            Integer todoId,
            Integer documentId,
            String documentName,
            List<IssuerSummary> issuers,
            boolean isChecked) {}
}
