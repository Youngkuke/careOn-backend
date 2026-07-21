package com.youngkke.careon.domain.carer.dto;

/** GET /api/web/users/me 응답 body. */
public record WebMeResponse(
        Integer carerId,
        String name,
        String email,
        String region,
        boolean diagnosisCompleted,
        boolean appInstalled,
        int installPromptCount,
        boolean notificationEnabled) {}
