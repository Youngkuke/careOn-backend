package com.youngkke.careon.domain.carer.dto;

/** POST /api/web/users/login 응답 body. 웹은 refresh_token을 발급하지 않는다. */
public record WebLoginResponse(Integer carerId, String accessToken, boolean diagnosisCompleted) {}
