package com.youngkke.careon.domain.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentIssueRepository extends JpaRepository<DocumentIssue, Integer> {

    List<DocumentIssue> findByDocument(Document document);
}
