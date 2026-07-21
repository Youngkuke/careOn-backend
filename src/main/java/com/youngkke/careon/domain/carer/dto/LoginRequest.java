package com.youngkke.careon.domain.carer.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /api/web/users/login, POST /api/app/users/login 공통 요청 body. */
public record LoginRequest(

        @NotBlank(message = "이메일과 비밀번호를 입력해주세요.")
        String email,

        @NotBlank(message = "이메일과 비밀번호를 입력해주세요.")
        String password
) {
}
