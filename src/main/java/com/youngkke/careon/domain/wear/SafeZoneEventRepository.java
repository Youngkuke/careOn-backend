package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SafeZoneEventRepository extends JpaRepository<SafeZoneEvent, Integer> {

    Optional<SafeZoneEvent> findByWearDeviceAndIdempotencyKey(WearDevice wearDevice, String idempotencyKey);

    Optional<SafeZoneEvent> findBySafeZoneEventIdAndWearDevice(Integer safeZoneEventId, WearDevice wearDevice);

    /** 워치 사용자가 아직 응답하지 않은 가장 최근 이탈 이벤트. 보호자 모바일의 "활성 이탈 이벤트" 조회용. */
    Optional<SafeZoneEvent> findFirstByCaredAndResponseIsNullOrderByDetectedAtDesc(Cared cared);

    /**
     * 아직 응답이 없고 마감도 지나지 않은 가장 최근 이탈 이벤트. 워치가 재시작된 뒤 화면 복원에 쓴다.
     * 마감을 함께 보는 이유는, 무응답 확정 배치가 도는 사이(최대 배치 주기만큼)에도 이미 마감된 이벤트를
     * "아직 응답할 수 있는 것"처럼 워치에 되돌려주지 않기 위해서다.
     */
    Optional<SafeZoneEvent> findFirstByCaredAndResponseIsNullAndDetectedAtAfterOrderByDetectedAtDesc(
            Cared cared, LocalDateTime detectedAfter);

    /** 응답 마감이 지났는데 아직 응답이 없는 이벤트들. 무응답 확정 배치용. */
    List<SafeZoneEvent> findAllByResponseIsNullAndDetectedAtBefore(LocalDateTime detectedBefore);

    /** 이탈 이력 첫 페이지. 최신순(detectedAt 내림차순), 같은 시각이면 id 내림차순. */
    List<SafeZoneEvent> findAllByCaredOrderByDetectedAtDescSafeZoneEventIdDesc(Cared cared, Pageable pageable);

    /** 이탈 이력 다음 페이지. 커서가 가리키는 (시각, id)보다 뒤에 오는 것만 가져온다. */
    @Query(
            """
            SELECT e FROM SafeZoneEvent e
            WHERE e.cared = :cared
              AND (e.detectedAt < :timestamp
                   OR (e.detectedAt = :timestamp AND e.safeZoneEventId < :id))
            ORDER BY e.detectedAt DESC, e.safeZoneEventId DESC
            """)
    List<SafeZoneEvent> findPageAfter(
            @Param("cared") Cared cared,
            @Param("timestamp") LocalDateTime timestamp,
            @Param("id") Integer id,
            Pageable pageable);
}
