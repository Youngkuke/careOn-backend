package com.youngkke.careon.domain.policy.dto;

import jakarta.validation.constraints.NotNull;

public record SavePolicyRequest(@NotNull(message = "값이 누락되었습니다.") Integer policyId) {}
