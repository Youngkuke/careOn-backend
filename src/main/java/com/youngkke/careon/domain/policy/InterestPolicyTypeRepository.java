package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestPolicyTypeRepository extends JpaRepository<InterestPolicyType, Integer> {

    void deleteAllByCarer(Carer carer);

    /** 유저의 관심 유형을 등록 순서(id 오름차순)로 조회한다. 대표 유형 선정 시 이 순서를 우선한다. */
    @Query("""
            select i from InterestPolicyType i
            join fetch i.policyType
            where i.carer = :carer
            order by i.interestPolicyTypeId
            """)
    List<InterestPolicyType> findAllWithTypeByCarer(@Param("carer") Carer carer);
}
