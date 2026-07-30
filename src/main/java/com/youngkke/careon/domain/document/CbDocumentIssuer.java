package com.youngkke.careon.domain.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * cb 제도 서류 이름 -> 발급처 연결.
 *
 * <p>cb 서류는 AI가 제도마다 생성한 자유 텍스트라 documents 테이블에 행이 없다. 그래서 발급처를 붙이려면
 * documents에 행을 만들어야 할 것 같지만, 그렇게 하면 안 된다. 표기만 조금씩 다른 중복 행이 계속 쌓여
 * 마스터 품질이 무너진다. (실제로 "개인정보동의서 / 개인정보제공 동의서 / 개인정보 수집·이용 및 제공
 * 동의서 / 개인정보수집 및 이용·제공 동의서"가 이미 따로 들어와 있다)
 *
 * <p>그래서 documents는 그대로 두고, 이름과 발급처만 잇는 표를 따로 둔다. 발급처 자체는
 * document_issuers를 그대로 쓴다. 발급처 목록이 두 벌로 갈라지면 안 되기 때문이다.
 *
 * <p>이름을 정확히 일치시킬 때만 쓴다. "~진단서면 병원" 같은 패턴 규칙을 넣지 않는 이유는, 틀려도
 * 조용히 틀리기 때문이다. (예: "국민연금 수급자 확인서"는 확인서지만 병원이 아니라 국민연금공단이다)
 * 매핑에 없는 이름은 발급처가 비는데, 그건 지금과 같은 상태라 더 나빠지지 않는다.
 */
@Entity
@Table(name = "cb_document_issuers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CbDocumentIssuer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cb_document_issuer_id")
    private Integer cbDocumentIssuerId;

    /** cb가 준 서류 이름. 표기까지 그대로 맞춰야 걸린다. */
    @Column(name = "document_name", nullable = false, length = 200)
    private String documentName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_issuer_id", nullable = false)
    private DocumentIssuer documentIssuer;
}
