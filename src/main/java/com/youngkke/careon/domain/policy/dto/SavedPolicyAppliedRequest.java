package com.youngkke.careon.domain.policy.dto;

import jakarta.validation.constraints.NotNull;

/** 마감 지난 저장 제도의 신청 여부 응답 요청. */
public record SavedPolicyAppliedRequest(@NotNull(message = "applied 값을 입력해주세요.") Boolean applied) {}
