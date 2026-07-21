package com.youngkke.careon.domain.carer.dto;

/** GET /api/web/users/me 응답 body. 명세서 필드명이 district라 그대로 따랐다(내부 컬럼명은 region). */
public record WebMeResponse(
        Integer userId,
        String name,
        String email,
        String district,
        boolean diagnosisCompleted,
        boolean appInstalled,
        int installPromptCount
) {
}
