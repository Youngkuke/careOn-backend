package com.youngkke.careon.domain.carer.dto;

/** PATCH /api/web/users/me/app-install-status 응답 body. */
public record AppInstallStatusResponse(String message, boolean appInstalled, int installPromptCount) {}
