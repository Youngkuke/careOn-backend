package com.youngkke.careon.domain.carer.dto;

/** GET /api/app/users/me 응답 body. */
public record AppMeResponse(
        Integer userId,
        String name,
        String email,
        String region,
        boolean notificationEnabled
) {
}
