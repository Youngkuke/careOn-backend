package com.youngkke.careon.domain.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** ERD의 "서류 발급처(document_issuer)" 테이블. */
@Entity
@Table(name = "document_issuers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DocumentIssuer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_issuer_id")
    private Integer documentIssuerId;

    @Column(name = "issuer_name", nullable = false, length = 100)
    private String issuerName;

    @Column(name = "issuer_site", length = 500)
    private String issuerSite;
}
