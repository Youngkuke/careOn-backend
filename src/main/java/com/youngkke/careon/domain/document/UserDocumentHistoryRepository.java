package com.youngkke.careon.domain.document;

import com.youngkke.careon.domain.carer.Carer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDocumentHistoryRepository extends JpaRepository<UserDocumentHistory, Integer> {

    /** 유저의 서류 이력을 서류/제도까지 즉시 로딩해서 조회한다. */
    @Query("select h from UserDocumentHistory h join fetch h.document join fetch h.policy where h.carer = :carer")
    List<UserDocumentHistory> findAllWithDetailByCarer(@Param("carer") Carer carer);

    Optional<UserDocumentHistory> findByHistoryIdAndCarer(Integer historyId, Carer carer);
}
