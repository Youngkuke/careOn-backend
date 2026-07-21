package com.youngkke.careon.domain.policy.dto;

import jakarta.validation.constraints.NotNull;

/** 매칭 제도 수혜 여부 수정 요청. */
public record WasBenefitedRequest(@NotNull(message = "값이 누락되었습니다.") Boolean wasBenefited) {}
