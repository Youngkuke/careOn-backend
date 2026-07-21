package com.youngkke.careon.domain.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentIssueRepository extends JpaRepository<DocumentIssue, Integer> {

    List<DocumentIssue> findByDocument(Document document);

    /** 여러 서류의 발급처를 한 번에 조회한다 (N+1 방지). */
    @Query("select i from DocumentIssue i join fetch i.documentIssuer where i.document in :documents")
    List<DocumentIssue> findAllWithIssuerByDocumentIn(@Param("documents") List<Document> documents);
}
