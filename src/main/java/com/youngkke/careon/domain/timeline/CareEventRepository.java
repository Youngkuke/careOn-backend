package com.youngkke.careon.domain.timeline;

import com.youngkke.careon.domain.carer.Cared;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CareEventRepository extends JpaRepository<CareEvent, Integer> {

    /** 타임라인 첫 페이지. 최신순(occurredAt 내림차순), 같은 시각이면 id 내림차순. */
    List<CareEvent> findAllByCaredOrderByOccurredAtDescCareEventIdDesc(Cared cared, Pageable pageable);

    /** 타임라인 다음 페이지. 커서가 가리키는 (시각, id)보다 뒤에 오는 것만 가져온다. */
    @Query(
            """
            SELECT e FROM CareEvent e
            WHERE e.cared = :cared
              AND (e.occurredAt < :timestamp
                   OR (e.occurredAt = :timestamp AND e.careEventId < :id))
            ORDER BY e.occurredAt DESC, e.careEventId DESC
            """)
    List<CareEvent> findPageAfter(
            @Param("cared") Cared cared,
            @Param("timestamp") LocalDateTime timestamp,
            @Param("id") Integer id,
            Pageable pageable);

    /**
     * 회원 탈퇴 정리용. 엔티티를 하나씩 읽어 지우지 않고 한 번에 지운다.
     * 타임라인은 계속 쌓이는 데이터라 탈퇴 한 번에 메모리로 다 올리면 안 된다.
     */
    @Modifying
    @Query("delete from CareEvent e where e.cared in :caredList")
    void deleteAllByCaredIn(@Param("caredList") List<Cared> caredList);
}
