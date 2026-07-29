package com.youngkke.careon.domain.todo.dto;

import com.youngkke.careon.domain.document.dto.IssuerSummary;
import java.util.List;

/**
 * 투두 목록 조회(앱) 응답 항목 (저장한 제도 하나 단위).
 *
 * <p>제도가 두 갈래라 기존 제도는 policyId가, cb 제도는 servId가 채워진다. 둘 중 하나는 항상 null이다.
 */
public record TodoListResponse(
        Integer savedPolicyId,
        Integer policyId,
        String servId,
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

    /**
     * 서류 1건.
     *
     * <p>cb 제도의 서류는 우리 documents 테이블에 없어 documentId가 null이고, 발급처 목록 대신
     * documentUrl 하나가 내려간다. 기존 제도는 반대로 documentUrl이 null이고 issuers가 채워진다.
     *
     * @param documentUrlType certificate_issuance(발급처) 또는 form_download(서식 내려받기)
     */
    public record TodoDocumentDetail(
            Integer todoId,
            Integer documentId,
            String documentName,
            List<IssuerSummary> issuers,
            String documentUrl,
            String documentUrlType,
            boolean isChecked) {}
}
