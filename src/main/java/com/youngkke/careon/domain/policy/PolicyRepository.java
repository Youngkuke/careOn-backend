package com.youngkke.careon.domain.policy;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Integer> {

    List<Policy> findByCategoryAndPolicyType_PolicyTypeIdIn(PolicyCategory category, List<Integer> policyTypeIds);
}
