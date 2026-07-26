package com.youngkke.careon.domain.policy.dto;

/**
 * 제도 저장 요청. 기존 제도는 policyId로, cb(복지로) 제도는 servId로 저장한다.
 * 둘 중 정확히 하나만 보내야 하며, 어느 쪽도 단독 필수가 아니라 애노테이션 대신 서비스에서 검증한다.
 */
public record SavePolicyRequest(Integer policyId, String servId) {

    public boolean hasExactlyOneTarget() {
        return (policyId != null) ^ (servId != null && !servId.isBlank());
    }
}
