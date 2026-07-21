package com.youngkke.careon.domain.document;

import com.youngkke.careon.domain.policy.Policy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectPolicyDocumentRepository extends JpaRepository<ConnectPolicyDocument, Integer> {

    List<ConnectPolicyDocument> findByPolicy(Policy policy);

    /** 서류 상세에서 "이 서류를 요구하는 제도 목록"을 조회한다. */
    @Query("select c from ConnectPolicyDocument c join fetch c.policy where c.document = :document")
    List<ConnectPolicyDocument> findAllWithPolicyByDocument(@Param("document") Document document);
}
