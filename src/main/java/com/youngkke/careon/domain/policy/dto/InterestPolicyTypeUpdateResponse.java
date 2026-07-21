package com.youngkke.careon.domain.policy.dto;

import java.util.List;

/** 내 관심 유형 수정 응답. */
public record InterestPolicyTypeUpdateResponse(String message, List<Integer> interestPolicyTypeIds) {}
