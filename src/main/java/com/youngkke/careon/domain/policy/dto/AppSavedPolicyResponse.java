package com.youngkke.careon.domain.policy.dto;

import java.util.List;

/**
 * 저장한 제도 목록 조회(앱 캘린더) 응답 항목.
 * 지난 날짜도 계속 내려주되 D+ 표기는 하지 않는다. (지난 항목은 d_day가 null, 마감 지난 건 documents도 빈 배열)
 */
public record AppSavedPolicyResponse(
        Integer savedPolicyId,
        Integer policyId,
        String servId,
        String policyName,
        String applicationDeadline,
        String applicationDeadlineDDay,
        List<String> documents,
        String resultDate,
        String resultDateDDay) {}
