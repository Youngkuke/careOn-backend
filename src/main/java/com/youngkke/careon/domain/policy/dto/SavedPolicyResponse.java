package com.youngkke.careon.domain.policy.dto;

import com.youngkke.careon.domain.document.dto.DocumentSummary;
import java.util.List;

/**
 * 저장한 제도 목록 조회(웹) 응답 항목.
 * 카드 표시/상세 진입 정보에 더해, 모바일에서 신청 완료한 제도를 웹에서 구분하고 수혜 여부를
 * 입력할 수 있도록 신청/수혜 상태를 함께 반환한다. (is_checked 서류 체크 여부는 앱 전용이라 제외)
 */
public record SavedPolicyResponse(
        Integer savedPolicyId,
        Integer policyId,
        Integer matchedPolicyId,
        String policyName,
        Integer agencyId,
        String agencyName,
        String summary,
        String supportPeriod,
        String applicationDeadline,
        boolean isApplied,
        String applicationStatus,
        String appliedAt,
        String resultDate,
        String benefitStatus,
        String benefitCheckedAt,
        String link,
        List<PolicyTypeSummary> policyTypes,
        List<DocumentSummary> documents) {}
