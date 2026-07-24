package com.youngkke.careon.domain.policy.dto;

import com.youngkke.careon.domain.policy.BenefitStatus;
import jakarta.validation.constraints.NotNull;

/** 저장한 제도의 수혜 여부 수정 요청 (웹 전용). */
public record BenefitStatusRequest(
        @NotNull(message = "benefit_status 값을 입력해주세요.") BenefitStatus benefitStatus) {}
