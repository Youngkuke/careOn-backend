package com.youngkke.careon.domain.policy;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository extends JpaRepository<Policy, Integer> {

    /**
     * 제도 목록 검색. 모든 조건은 선택이며, 유형 필터는 다대다 연결 테이블을 통해 적용한다.
     * hasTypeFilter가 false면 typeIds 조건은 무시된다. (JPQL의 IN 절에 빈 목록을 넘기지 않기 위한 처리)
     */
    @Query("""
            select distinct p from Policy p
            join fetch p.agency a
            where (:category is null or p.category = :category)
              and (:agencyId is null or a.agencyId = :agencyId)
              and (cast(:keyword as string) is null
                   or lower(p.policyName) like lower(concat('%', cast(:keyword as string), '%'))
                   or lower(p.summary) like lower(concat('%', cast(:keyword as string), '%')))
              and (:hasTypeFilter = false
                   or p.policyId in (select c.policy.policyId from ConnectPolicyPolicyType c
                                     where c.policyType.policyTypeId in :typeIds))
            order by p.policyId
            """)
    List<Policy> search(
            @Param("category") PolicyCategory category,
            @Param("agencyId") Integer agencyId,
            @Param("keyword") String keyword,
            @Param("hasTypeFilter") boolean hasTypeFilter,
            @Param("typeIds") List<Integer> typeIds);

    /** ID 목록으로 제도를 조회한다 (기관 즉시 로딩). */
    @Query("select p from Policy p join fetch p.agency where p.policyId in :ids order by p.policyId")
    List<Policy> findAllWithAgencyByIdIn(@Param("ids") List<Integer> ids);

    /** 기관에 연결된 제도 목록. */
    List<Policy> findAllByAgency_AgencyIdOrderByPolicyIdAsc(Integer agencyId);
}
