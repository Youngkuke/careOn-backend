package com.youngkke.careon.domain.carer.dto;

/** POST /api/web/users/login 응답 body. */
public record WebLoginResponse(Integer userId, String accessToken, boolean diagnosisCompleted) {
}
