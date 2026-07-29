package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HeartRateRepository extends JpaRepository<HeartRate, Integer> {

    Optional<HeartRate> findByWearDeviceAndIdempotencyKey(WearDevice wearDevice, String idempotencyKey);

    /**
     * 가장 최근 측정값. 서버 수신 순서가 아니라 측정 시각 기준이다.
     * 워치가 오프라인이었다가 밀린 값을 몰아서 올리면 나중에 도착한 게 더 옛날 측정값일 수 있기 때문이다.
     */
    Optional<HeartRate> findFirstByCaredOrderByMeasuredAtDescHeartRateIdDesc(Cared cared);

    /**
     * 회원 탈퇴 정리용. 심박수는 워치가 주기적으로 올려 가장 빠르게 쌓이는 데이터라
     * 엔티티로 읽어들이지 않고 한 번에 지운다.
     */
    @Modifying
    @Query("delete from HeartRate h where h.cared in :caredList")
    void deleteAllByCaredIn(@Param("caredList") List<Cared> caredList);
}
