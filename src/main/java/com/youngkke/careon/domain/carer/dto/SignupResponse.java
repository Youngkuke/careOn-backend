package com.youngkke.careon.domain.carer.dto;

/** POST /api/web/users/register 응답 body (201 Created). */
public record SignupResponse(Integer userId, String accessToken, boolean diagnosisCompleted) {
}
