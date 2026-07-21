package com.youngkke.careon.domain.policy.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 내 관심 유형 수정 요청 (전체 교체). */
public record InterestPolicyTypeUpdateRequest(
        @NotEmpty(message = "관심 제도 유형을 1개 이상 선택해주세요.") List<Integer> interestPolicyTypeIds) {}
