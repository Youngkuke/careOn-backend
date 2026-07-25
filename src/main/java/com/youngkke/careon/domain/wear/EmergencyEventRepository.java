package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Integer> {

    Optional<EmergencyEvent> findByWearDeviceAndIdempotencyKey(WearDevice wearDevice, String idempotencyKey);

    Optional<EmergencyEvent> findFirstByCaredAndStatusOrderByRequestedAtDesc(Cared cared, EmergencyStatus status);

    List<EmergencyEvent> findAllByCaredAndStatus(Cared cared, EmergencyStatus status);

    Optional<EmergencyEvent> findByEmergencyEventIdAndCared(Integer emergencyEventId, Cared cared);
}
