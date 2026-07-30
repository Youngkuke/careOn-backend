package com.youngkke.careon.domain.document.dto;

import com.youngkke.careon.domain.document.DocumentIssuer;

/**
 * 서류 발급처 요약.
 *
 * @param issueGuide 화면에 그대로 쓸 한 문장("진료받은 병원에서 발급 가능"). 기관 이름만으로는 무엇을
 *     해야 하는지 안 드러나서 따로 둔다. 아직 문구를 안 채운 발급처면 null이라 issuerName으로 대체한다.
 */
public record IssuerSummary(Integer documentIssuerId, String issuerName, String issuerSite, String issueGuide) {

    public static IssuerSummary from(DocumentIssuer issuer) {
        return new IssuerSummary(
                issuer.getDocumentIssuerId(), issuer.getIssuerName(), issuer.getIssuerSite(), issuer.getIssueGuide());
    }
}
