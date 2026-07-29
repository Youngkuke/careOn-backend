package com.youngkke.careon.domain.todo;

import com.youngkke.careon.domain.document.Document;
import com.youngkke.careon.domain.policy.SavedPolicy;
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
 * ERD의 "투두리스트(todo)" 테이블. 저장한 제도에 필요한 서류별 체크 상태.
 *
 * <p>서류가 두 갈래다. 기존 제도는 우리가 정제해 관리하는 documents 테이블의 행을 document로 가리키고,
 * cb 제도는 AI 서버가 제도마다 생성해 둔 문자열이라 가리킬 행이 없어 이름을 그대로 들고 있는다.
 *
 * <p>cb 서류를 documents에 만들어 넣지 않는 이유는, 그 테이블이 발급처(document_issuers)까지 연결된
 * 마스터 데이터이기 때문이다. AI 생성 자유 텍스트를 섞으면 표기가 조금씩 다른 중복 행이 계속 쌓이고,
 * 기존 제도 쪽 서류 품질까지 같이 나빠진다.
 */
@Entity
@Table(name = "todos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "todo_id")
    private Integer todoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_policy_id", nullable = false)
    private SavedPolicy savedPolicy;

    /** 기존 제도의 서류. cb 제도의 서류면 null이고 documentName이 대신 채워진다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    /** cb 제도의 서류 이름. 기존 제도의 서류면 null이다. */
    @Column(name = "document_name")
    private String documentName;

    /** cb 제도 서류의 발급처/서식 링크. 없을 수 있다. */
    @Column(name = "document_url")
    private String documentUrl;

    /** documentUrl의 성격. certificate_issuance(발급처) 또는 form_download(서식 내려받기). */
    @Column(name = "document_url_type", length = 40)
    private String documentUrlType;

    @Column(name = "is_checked", nullable = false)
    private boolean checked;

    /** cb 제도의 서류인지. 응답을 만들 때 어느 쪽에서 이름을 읽을지 가른다. */
    public boolean isCbDocument() {
        return document == null;
    }

    /** 어느 갈래든 화면에 보여줄 서류 이름. */
    public String resolveDocumentName() {
        return isCbDocument() ? documentName : document.getDocumentName();
    }

    public void updateChecked(boolean checked) {
        this.checked = checked;
    }
}
