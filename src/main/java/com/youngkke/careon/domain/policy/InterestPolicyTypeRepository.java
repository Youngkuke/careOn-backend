package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestPolicyTypeRepository extends JpaRepository<InterestPolicyType, Integer> {

    void deleteAllByCarer(Carer carer);
}
