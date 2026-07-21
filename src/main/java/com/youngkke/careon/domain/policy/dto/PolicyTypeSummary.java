package com.youngkke.careon.domain.policy.dto;

import com.youngkke.careon.domain.policy.PolicyType;

/** 제도 유형 요약. 응답의 policy_types 배열 항목으로 사용한다. */
public record PolicyTypeSummary(Integer policyTypeId, String typeName) {

    public static PolicyTypeSummary from(PolicyType policyType) {
        return new PolicyTypeSummary(policyType.getPolicyTypeId(), policyType.getTypeName());
    }
}
