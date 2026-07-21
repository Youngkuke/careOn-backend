package com.youngkke.careon.domain.policy;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectPolicyPolicyTypeRepository extends JpaRepository<ConnectPolicyPolicyType, Integer> {

    /** 여러 제도의 유형 연결을 한 번에 조회한다 (N+1 방지, policyType까지 즉시 로딩). */
    @Query("select c from ConnectPolicyPolicyType c join fetch c.policyType where c.policy in :policies")
    List<ConnectPolicyPolicyType> findAllWithTypeByPolicyIn(@Param("policies") List<Policy> policies);

    @Query("select c from ConnectPolicyPolicyType c join fetch c.policyType where c.policy = :policy")
    List<ConnectPolicyPolicyType> findAllWithTypeByPolicy(@Param("policy") Policy policy);
}
