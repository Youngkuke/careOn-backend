package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPolicyRepository extends JpaRepository<SavedPolicy, Integer> {

    List<SavedPolicy> findAllByCarer(Carer carer);

    void deleteAllByCarer(Carer carer);

    boolean existsByCarerAndPolicy(Carer carer, Policy policy);

    Optional<SavedPolicy> findBySavedPolicyIdAndCarer(Integer savedPolicyId, Carer carer);
}
