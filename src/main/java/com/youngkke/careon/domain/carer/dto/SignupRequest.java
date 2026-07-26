package com.youngkke.careon.domain.carer.dto;

import com.youngkke.careon.global.validation.ValidationPatterns;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

        /**
         * 관심 제도 유형. 선택 항목이며 생략하거나 빈 배열로 보내도 된다.
         *
         * <p>이 값은 맞춤 제도 목록을 어떤 유형으로 묶어 보여줄지 정하는 정렬 기준으로만 쓰인다.
         * 비어 있으면 제도가 가진 첫 유형으로 묶일 뿐, 가입이나 기능 이용에는 영향이 없다.
         * 가입 후 PATCH /api/web/users/me/interest-policy-types 로 언제든 설정할 수 있어
         * 가입 시점에 강제할 이유가 없다.
         */
        @Size(max = 4, message = "관심 제도 유형은 4개 이하로 선택해주세요.")
        List<Integer> interestPolicyTypeIds
) {

    /** 생략(null)과 빈 배열을 같게 다룬다. */
    public List<Integer> interestPolicyTypeIdsOrEmpty() {
        return interestPolicyTypeIds == null ? List.of() : interestPolicyTypeIds;
    }
}
