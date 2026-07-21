package com.youngkke.careon.domain.document;

import com.youngkke.careon.domain.policy.Policy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectPolicyDocumentRepository extends JpaRepository<ConnectPolicyDocument, Integer> {

    List<ConnectPolicyDocument> findByPolicy(Policy policy);
}
