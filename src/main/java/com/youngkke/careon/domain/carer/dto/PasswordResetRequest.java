package com.youngkke.careon.domain.carer.dto;

import com.youngkke.careon.global.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** POST /api/web/users/password/reset 요청 body. */
public record PasswordResetRequest(

        @NotBlank(message = "잘못된 접근입니다.")
        String resetToken,

        @NotBlank(message = "비밀번호는 영문과 숫자를 포함하여 8~20자여야 합니다.")
        @Pattern(regexp = ValidationPatterns.PASSWORD, message = "비밀번호는 영문과 숫자를 포함하여 8~20자여야 합니다.")
        String newPassword
) {
}
