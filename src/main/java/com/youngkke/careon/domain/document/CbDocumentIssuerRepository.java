package com.youngkke.careon.domain.document;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CbDocumentIssuerRepository extends JpaRepository<CbDocumentIssuer, Integer> {

    /** 여러 cb 서류 이름의 발급처를 한 번에 조회한다 (N+1 방지). */
    @Query("""
            select c from CbDocumentIssuer c
            join fetch c.documentIssuer
            where c.documentName in :names
            """)
    List<CbDocumentIssuer> findAllWithIssuerByDocumentNameIn(@Param("names") Collection<String> names);
}
