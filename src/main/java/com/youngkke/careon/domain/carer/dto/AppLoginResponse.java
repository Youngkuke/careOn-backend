package com.youngkke.careon.domain.carer.dto;

/** POST /api/app/users/login 응답 body. */
public record AppLoginResponse(Integer userId, String accessToken, String refreshToken) {
}
