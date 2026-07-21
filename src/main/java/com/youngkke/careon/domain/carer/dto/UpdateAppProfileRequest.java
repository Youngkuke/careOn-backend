package com.youngkke.careon.domain.carer.dto;

import com.youngkke.careon.global.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH /api/app/users/me 요청 body. 모든 필드 optional (앱은 화면마다 필드 하나씩만 보냄). */
public record UpdateAppProfileRequest(

        @Size(max = 50)
        String name,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255)
        String email,

        @Pattern(regexp = ValidationPatterns.PASSWORD, message = "비밀번호는 영문과 숫자를 포함하여 8~20자여야 합니다.")
        String password,

        @Size(max = 20)
        String region,

        Boolean notificationEnabled
) {
}
