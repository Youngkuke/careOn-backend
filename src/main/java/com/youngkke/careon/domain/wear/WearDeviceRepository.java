package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WearDeviceRepository extends JpaRepository<WearDevice, Integer> {

    Optional<WearDevice> findByCared(Cared cared);

    /** 회원 탈퇴 정리용. 피보호자당 한 대라 건별로 지워도 부담이 없다. */
    void deleteAllByCaredIn(List<Cared> caredList);

    /**
     * 오래 연락이 끊겼는데 아직 미통신 알림을 안 보낸 워치들. 미통신 알림 배치용.
     * 보호자가 스스로 연결을 해제한 기기(disconnectedAt != null)는 연락이 없는 게 당연하므로 제외한다.
     */
    List<WearDevice> findAllByDisconnectedAtIsNullAndOfflineNotifiedAtIsNullAndLastSeenAtBefore(
            LocalDateTime lastSeenBefore);

    /**
     * 워치 토큰으로 들어온 요청이 쓰는 조회. 해제된 기기는 여기서 막는다.
     * 워치 access token은 서명만 검증하는 JWT라(CurrentWearDeviceIdArgumentResolver) 보호자가 연결을 해제해도
     * 남은 유효기간(기본 1시간) 동안 그대로 통과한다. 워치 서비스들이 공통으로 타는 이 지점에서 걸러야
     * 엔드포인트가 새로 늘어나도 검사가 빠지지 않는다.
     */
    default WearDevice getConnectedOrThrow(Integer wearDeviceId) {
        WearDevice wearDevice =
                findById(wearDeviceId).orElseThrow(() -> new BusinessException(ErrorCode.WEAR_DEVICE_NOT_FOUND));
        if (wearDevice.isDisconnected()) {
            throw new BusinessException(ErrorCode.WEAR_DEVICE_DISCONNECTED);
        }
        return wearDevice;
    }
}
