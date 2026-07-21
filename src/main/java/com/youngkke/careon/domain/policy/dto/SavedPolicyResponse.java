package com.youngkke.careon.domain.policy.dto;

import com.youngkke.careon.domain.policy.dto.PolicyDetailResponse.DocumentDetail;
import java.util.List;

/** 저장한 제도 목록 조회(웹) 응답 항목. */
public record SavedPolicyResponse(
        Integer savedPolicyId,
        Integer policyId,
        String name,
        String policyType,
        String organization,
        String content,
        String deadline,
        String resultDate,
        List<DocumentDetail> documents) {}
