package com.youngkke.careon.domain.todo;

import com.youngkke.careon.domain.policy.SavedPolicy;
import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Integer> {

    void deleteAllBySavedPolicyIn(List<SavedPolicy> savedPolicies);

    /**
     * 여러 저장 제도의 투두를 한 번에 조회한다 (N+1 방지).
     * cb 제도의 투두는 document가 비어 있으므로 left join이어야 한다.
     */
    @Query("select t from Todo t left join fetch t.document where t.savedPolicy in :savedPolicies")
    List<Todo> findAllWithDocumentBySavedPolicyIn(@Param("savedPolicies") List<SavedPolicy> savedPolicies);

    Optional<Todo> findByTodoIdAndSavedPolicy_Carer(Integer todoId, Carer carer);
}
