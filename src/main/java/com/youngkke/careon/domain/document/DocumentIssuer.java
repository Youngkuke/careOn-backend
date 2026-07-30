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

    /**
     * 앱 화면에 그대로 쓸 한 문장. 예: "진료받은 병원에서 발급 가능"
     *
     * <p>issuerName은 기관 이름이라 화면에 그대로 박으면 "병·의원(의료기관)"처럼 사용자가 무엇을 해야
     * 하는지 안 알려준다. 그렇다고 앱에서 이름 뒤에 "에서 발급 가능"을 붙이게 하면 "따로 발급 불필요",
     * "본인 작성"처럼 기관이 아닌 값에서 문장이 깨진다. 그래서 문구를 데이터로 따로 들고 있는다.
     */
    @Column(name = "issue_guide", length = 100)
    private String issueGuide;
}
