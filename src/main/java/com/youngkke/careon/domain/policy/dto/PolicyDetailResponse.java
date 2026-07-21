package com.youngkke.careon.domain.policy.dto;

import java.util.List;

/** 제도 상세 조회 응답. */
public record PolicyDetailResponse(
        Integer policyId,
        String name,
        String policyType,
        String organization,
        String supportPeriod,
        String selfPayment,
        String deadline,
        String applicationMethod,
        String duration,
        String resultDate,
        String contact,
        List<DocumentDetail> documents) {

    public record DocumentDetail(Integer documentId, String name, List<IssuerDetail> issuers) {}

    public record IssuerDetail(String issuer, String site) {}
}
