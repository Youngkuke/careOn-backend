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

    /**
     * saved_policy와 matched_policy 사이에 FK가 없어서, (carer, policy) 쌍으로 최선 매칭한다.
     * 같은 (carer, policy) 조합에 매칭 결과가 여러 건 쌓일 수 있는 구조라면 이 조회는 안정적이지 않으니
     * 실제로 그런 케이스가 있는지 확인이 필요하다 (현재는 유니크 제약이 없음).
     */
    Optional<MatchedPolicy> findFirstByCarerAndPolicy(Carer carer, Policy policy);
}
