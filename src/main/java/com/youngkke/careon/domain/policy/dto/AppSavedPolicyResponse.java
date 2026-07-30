package com.youngkke.careon.domain.policy.dto;

import java.util.List;

/**
 * 저장한 제도 목록 조회(앱 캘린더) 응답 항목.
 * 지난 날짜도 계속 내려주되 D+ 표기는 하지 않는다. (지난 항목은 d_day가 null, 마감 지난 건 documents도 빈 배열)
 *
 * @param applicationPeriodType FIXED / ALWAYS_OPEN / UNKNOWN. 마감일이 없는 이유가 두 가지라 앱이 구분할 수
 *     있도록 함께 내려준다. 상시 신청이라 없는 것과, 마감일 정보를 우리가 못 채운 것은 뜻이 정반대다.
 *     두 경우를 같이 "상시"로 표시하면 실제로는 마감이 있는 제도를 놓치게 된다.
 */
public record AppSavedPolicyResponse(
        Integer savedPolicyId,
        Integer policyId,
        String servId,
        String policyName,
        String applicationDeadline,
        String applicationDeadlineDDay,
        String applicationPeriodType,
        List<String> documents,
        String resultDate,
        String resultDateDDay) {}
