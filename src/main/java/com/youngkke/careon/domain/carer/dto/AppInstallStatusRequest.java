package com.youngkke.careon.domain.carer.dto;

import jakarta.validation.constraints.NotNull;

/** PATCH /api/web/users/me/app-install-status 요청 body. */
public record AppInstallStatusRequest(

        @NotNull(message = "값이 누락되었습니다.")
        Boolean installed
) {
}
