package com.youngkke.careon.domain.wear;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafeZoneEventRepository extends JpaRepository<SafeZoneEvent, Integer> {

    Optional<SafeZoneEvent> findByWearDeviceAndIdempotencyKey(WearDevice wearDevice, String idempotencyKey);

    Optional<SafeZoneEvent> findBySafeZoneEventIdAndWearDevice(Integer safeZoneEventId, WearDevice wearDevice);
}
