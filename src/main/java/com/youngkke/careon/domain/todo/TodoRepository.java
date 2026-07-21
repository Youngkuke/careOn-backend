package com.youngkke.careon.domain.todo;

import com.youngkke.careon.domain.policy.SavedPolicy;
import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Integer> {

    void deleteAllBySavedPolicyIn(List<SavedPolicy> savedPolicies);

    List<Todo> findAllBySavedPolicy(SavedPolicy savedPolicy);

    Optional<Todo> findByTodoIdAndSavedPolicy_Carer(Integer todoId, Carer carer);
}
