package com.youngkke.careon.domain.carer.dto;

/** POST /api/web/users/register 응답 body (201 Created). 웹은 refresh_token을 발급하지 않는다. */
public record SignupResponse(Integer carerId, String accessToken, boolean diagnosisCompleted) {}
