package com.youngkke.careon.domain.carer.dto;

import com.youngkke.careon.global.validation.ValidationPatterns;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** POST /api/web/users/register 요청 body. */
public record SignupRequest(

        @NotBlank(message = "모든 항목을 입력해주세요.")
        @Size(max = 50)
        String name,

        @NotBlank(message = "모든 항목을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "모든 항목을 입력해주세요.")
        @Pattern(regexp = ValidationPatterns.PASSWORD, message = "비밀번호는 영문과 숫자를 포함하여 8~20자여야 합니다.")
        String password,

        @NotBlank(message = "모든 항목을 입력해주세요.")
        @Size(max = 20)
        String region,

        @NotNull(message = "모든 항목을 입력해주세요.")
        @AssertTrue(message = "이용약관 및 개인정보처리방침에 동의해야 합니다.")
        Boolean termsAgreed,

        @NotEmpty(message = "관심 제도 유형을 1개 이상 선택해주세요.")
        @Size(min = 1, max = 4, message = "관심 제도 유형은 1개 이상 4개 이하로 선택해주세요.")
        List<Integer> interestPolicyTypeIds
) {
}
