package com.youngkke.careon.domain.policy.dto;

import com.youngkke.careon.domain.document.dto.DocumentSummary;
import java.util.List;

/**
 * 저장한 제도 목록 조회(웹) 응답 항목.
 * 카드 표시와 상세 진입에 필요한 제도 정보만 반환한다. (applied, is_checked는 앱 전용이라 제외)
 */
public record SavedPolicyResponse(
        Integer savedPolicyId,
        Integer policyId,
        String policyName,
        Integer agencyId,
        String agencyName,
        String summary,
        String supportPeriod,
        String applicationDeadline,
        String link,
        List<PolicyTypeSummary> policyTypes,
        List<DocumentSummary> documents) {}
