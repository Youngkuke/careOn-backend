package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.wear.dto.LiveLocationResponse;
import com.youngkke.careon.domain.wear.dto.LiveLocationTrackingRequest;
import com.youngkke.careon.domain.wear.dto.LiveLocationTrackingResponse;
import com.youngkke.careon.domain.wear.dto.LiveLocationUpdateRequest;
import com.youngkke.careon.domain.wear.dto.LiveLocationUpdateResponse;
import com.youngkke.careon.domain.wear.dto.WearDeviceStatusResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 워치 연결 상태 조회와, 보호자가 켜고 끄는 실시간 위치 공유를 담당한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WearLiveLocationService {

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final WearDeviceRepository wearDeviceRepository;

    /** 보호자 모바일에서 워치 실제 연결 상태 조회. 연결된 적 없으면 null. */
    public WearDeviceStatusResponse getDeviceStatus(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        return findPrimaryCared(carer)
                .flatMap(wearDeviceRepository::findByCared)
                .map(this::toStatusResponse)
                .orElse(null);
    }

    /** 보호자가 실시간 위치 공유를 켜거나 끈다. 아직 연결된 워치가 없으면 에러. */
    @Transactional
    public LiveLocationTrackingResponse setTracking(Integer carerId, LiveLocationTrackingRequest request) {
        WearDevice wearDevice = getPairedWearDeviceOrThrow(carerId);
        LocalDateTime now = LocalDateTime.now();
        wearDevice.setLiveLocationEnabled(request.enabled(), now);
        return new LiveLocationTrackingResponse(wearDevice.isLiveLocationEnabled(), DateTimes.toIsoString(now));
    }

    /** 보호자 모바일에서 최신 위치 조회. 워치가 없거나 위치를 한 번도 안 보냈으면 null. */
    public LiveLocationResponse getLiveLocation(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        return findPrimaryCared(carer)
                .flatMap(wearDeviceRepository::findByCared)
                .filter(wearDevice -> wearDevice.getLiveCapturedAt() != null)
                .map(this::toLiveLocationResponse)
                .orElse(null);
    }

    /** 워치가 최신 위치를 전송. 추적이 꺼져 있으면 저장하지 않는다. */
    @Transactional
    public LiveLocationUpdateResponse updateLiveLocation(Integer wearDeviceId, LiveLocationUpdateRequest request) {
        WearDevice wearDevice = wearDeviceRepository
                .findById(wearDeviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WEAR_DEVICE_NOT_FOUND));

        if (!wearDevice.isLiveLocationEnabled()) {
            return new LiveLocationUpdateResponse(false);
        }

        wearDevice.updateLiveLocation(
                request.latitude(), request.longitude(), request.accuracyMeters(),
                DateTimes.parseToKst(request.capturedAt()));
        wearDevice.touchLastSeen(LocalDateTime.now());
        return new LiveLocationUpdateResponse(true);
    }

    private WearDevice getPairedWearDeviceOrThrow(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        Cared cared = findPrimaryCared(carer).orElseThrow(() -> new BusinessException(ErrorCode.WEAR_DEVICE_NOT_PAIRED));
        return wearDeviceRepository
                .findByCared(cared)
                .orElseThrow(() -> new BusinessException(ErrorCode.WEAR_DEVICE_NOT_PAIRED));
    }

    private Optional<Cared> findPrimaryCared(Carer carer) {
        return caredRepository.findAllByCarerOrderByCaredIdAsc(carer).stream().findFirst();
    }

    private WearDeviceStatusResponse toStatusResponse(WearDevice wearDevice) {
        return new WearDeviceStatusResponse(
                wearDevice.getWearDeviceId(),
                true,
                DateTimes.toIsoString(wearDevice.getConnectedAt()),
                DateTimes.toIsoString(wearDevice.getLastSeenAt()));
    }

    private LiveLocationResponse toLiveLocationResponse(WearDevice wearDevice) {
        return new LiveLocationResponse(
                wearDevice.isLiveLocationEnabled(),
                wearDevice.getLiveLatitude(),
                wearDevice.getLiveLongitude(),
                wearDevice.getLiveAccuracyMeters(),
                DateTimes.toIsoString(wearDevice.getLiveCapturedAt()));
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
