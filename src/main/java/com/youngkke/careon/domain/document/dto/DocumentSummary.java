package com.youngkke.careon.domain.document.dto;

import java.util.List;

/** 서류 요약 (발급처 포함). 제도 상세, 서류 목록/상세 응답에서 공용으로 쓴다. */
public record DocumentSummary(Integer documentId, String documentName, List<IssuerSummary> issuers) {}
