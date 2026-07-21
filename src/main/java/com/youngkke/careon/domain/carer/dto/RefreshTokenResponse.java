package com.youngkke.careon.domain.carer.dto;

/** POST /api/app/users/refresh 응답 body. */
public record RefreshTokenResponse(String accessToken, String refreshToken) {
}
