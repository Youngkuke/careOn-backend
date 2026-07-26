package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeartRateRepository extends JpaRepository<HeartRate, Integer> {

    Optional<HeartRate> findByWearDeviceAndIdempotencyKey(WearDevice wearDevice, String idempotencyKey);

    /**
     * 가장 최근 측정값. 서버 수신 순서가 아니라 측정 시각 기준이다.
     * 워치가 오프라인이었다가 밀린 값을 몰아서 올리면 나중에 도착한 게 더 옛날 측정값일 수 있기 때문이다.
     */
    Optional<HeartRate> findFirstByCaredOrderByMeasuredAtDescHeartRateIdDesc(Cared cared);
}
