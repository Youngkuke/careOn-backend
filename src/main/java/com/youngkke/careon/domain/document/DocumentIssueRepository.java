package com.youngkke.careon.domain.document;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentIssueRepository extends JpaRepository<DocumentIssue, Integer> {

    List<DocumentIssue> findByDocument(Document document);

    /** 여러 서류의 발급처를 한 번에 조회한다 (N+1 방지). */
    @Query("select i from DocumentIssue i join fetch i.documentIssuer where i.document in :documents")
    List<DocumentIssue> findAllWithIssuerByDocumentIn(@Param("documents") List<Document> documents);

    /**
     * 서류 "이름"으로 발급처를 조회한다. cb 제도의 서류는 documents 테이블의 행을 가리키지 않고 이름만
     * 들고 있어서, 같은 이름의 서류가 우리 마스터에 있으면 그 발급처를 빌려 쓰기 위한 것이다.
     * (마스터에 행을 새로 만들지는 않는다. 읽기만 한다)
     */
    @Query("""
            select i from DocumentIssue i
            join fetch i.documentIssuer
            join fetch i.document d
            where d.documentName in :names
            """)
    List<DocumentIssue> findAllWithIssuerByDocumentNameIn(@Param("names") Collection<String> names);
}
