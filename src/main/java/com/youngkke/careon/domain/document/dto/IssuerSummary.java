package com.youngkke.careon.domain.document.dto;

import com.youngkke.careon.domain.document.DocumentIssuer;

/** 서류 발급처 요약. */
public record IssuerSummary(Integer documentIssuerId, String issuerName, String issuerSite) {

    public static IssuerSummary from(DocumentIssuer issuer) {
        return new IssuerSummary(issuer.getDocumentIssuerId(), issuer.getIssuerName(), issuer.getIssuerSite());
    }
}
