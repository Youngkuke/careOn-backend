package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchedPolicyRepository extends JpaRepository<MatchedPolicy, Integer> {

    /** 유저의 매칭 결과를 제도/기관까지 즉시 로딩해서 조회한다. */
    @Query("select m from MatchedPolicy m join fetch m.policy p join fetch p.agency where m.carer = :carer")
    List<MatchedPolicy> findAllWithPolicyByCarer(@Param("carer") Carer carer);

    Optional<MatchedPolicy> findByMatchedPolicyIdAndCarer(Integer matchedPolicyId, Carer carer);
}
