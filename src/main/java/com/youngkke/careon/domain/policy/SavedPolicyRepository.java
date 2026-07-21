package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedPolicyRepository extends JpaRepository<SavedPolicy, Integer> {

    List<SavedPolicy> findAllByCarer(Carer carer);

    /** 저장 제도를 제도/기관까지 즉시 로딩해서 조회한다. */
    @Query("select s from SavedPolicy s join fetch s.policy p join fetch p.agency where s.carer = :carer")
    List<SavedPolicy> findAllWithPolicyByCarer(@Param("carer") Carer carer);

    void deleteAllByCarer(Carer carer);

    boolean existsByCarerAndPolicy(Carer carer, Policy policy);

    Optional<SavedPolicy> findBySavedPolicyIdAndCarer(Integer savedPolicyId, Carer carer);
}
