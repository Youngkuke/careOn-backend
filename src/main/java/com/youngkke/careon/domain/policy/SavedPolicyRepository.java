package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedPolicyRepository extends JpaRepository<SavedPolicy, Integer> {

    List<SavedPolicy> findAllByCarer(Carer carer);

    /**
     * 저장 제도를 제도/기관까지 즉시 로딩해서 조회한다.
     * cb 제도를 찜한 항목은 policy가 비어 있으므로 left join이어야 한다. 내부 조인이면 그 항목들이 목록에서 통째로 빠진다.
     */
    @Query("select s from SavedPolicy s left join fetch s.policy p left join fetch p.agency where s.carer = :carer")
    List<SavedPolicy> findAllWithPolicyByCarer(@Param("carer") Carer carer);

    void deleteAllByCarer(Carer carer);

    boolean existsByCarerAndPolicy(Carer carer, Policy policy);

    boolean existsByCarerAndServId(Carer carer, String servId);

    Optional<SavedPolicy> findBySavedPolicyIdAndCarer(Integer savedPolicyId, Carer carer);
}
