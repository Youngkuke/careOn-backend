package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafeZoneEventRepository extends JpaRepository<SafeZoneEvent, Integer> {

    Optional<SafeZoneEvent> findByWearDeviceAndIdempotencyKey(WearDevice wearDevice, String idempotencyKey);

    Optional<SafeZoneEvent> findBySafeZoneEventIdAndWearDevice(Integer safeZoneEventId, WearDevice wearDevice);

    /** 워치 사용자가 아직 응답하지 않은 가장 최근 이탈 이벤트. 보호자 모바일의 "활성 이탈 이벤트" 조회용. */
    Optional<SafeZoneEvent> findFirstByCaredAndResponseIsNullOrderByDetectedAtDesc(Cared cared);
}
