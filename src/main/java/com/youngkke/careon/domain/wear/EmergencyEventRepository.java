package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Integer> {

    Optional<EmergencyEvent> findByWearDeviceAndIdempotencyKey(WearDevice wearDevice, String idempotencyKey);

    Optional<EmergencyEvent> findFirstByCaredAndStatusOrderByRequestedAtDesc(Cared cared, EmergencyStatus status);

    List<EmergencyEvent> findAllByCaredAndStatus(Cared cared, EmergencyStatus status);

    Optional<EmergencyEvent> findByEmergencyEventIdAndCared(Integer emergencyEventId, Cared cared);

    /** SOS 이력 첫 페이지. 최신순(requestedAt 내림차순), 같은 시각이면 id 내림차순. */
    List<EmergencyEvent> findAllByCaredOrderByRequestedAtDescEmergencyEventIdDesc(Cared cared, Pageable pageable);

    /** SOS 이력 다음 페이지. 커서가 가리키는 (시각, id)보다 뒤에 오는 것만 가져온다. */
    @Query(
            """
            SELECT e FROM EmergencyEvent e
            WHERE e.cared = :cared
              AND (e.requestedAt < :timestamp
                   OR (e.requestedAt = :timestamp AND e.emergencyEventId < :id))
            ORDER BY e.requestedAt DESC, e.emergencyEventId DESC
            """)
    List<EmergencyEvent> findPageAfter(
            @Param("cared") Cared cared,
            @Param("timestamp") LocalDateTime timestamp,
            @Param("id") Integer id,
            Pageable pageable);

    /**
     * 회원 탈퇴 정리용. SOS가 발생할 때마다 쌓이는 데이터라 한 번에 지운다.
     * acknowledged_by도 carers를 참조하지만, 보호자는 자기 피보호자의 SOS만 확인할 수 있어
     * 피보호자 기준으로 다 걸린다.
     */
    @Modifying
    @Query("delete from EmergencyEvent e where e.cared in :caredList")
    void deleteAllByCaredIn(@Param("caredList") List<Cared> caredList);
}
