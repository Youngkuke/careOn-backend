package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.timeline.CareEventRecorder;
import com.youngkke.careon.domain.timeline.CareEventType;
import com.youngkke.careon.domain.wear.dto.LiveLocationResponse;
import com.youngkke.careon.domain.wear.dto.LiveLocationTrackingRequest;
import com.youngkke.careon.domain.wear.dto.LiveLocationTrackingResponse;
import com.youngkke.careon.domain.wear.dto.LiveLocationTrackingStatusResponse;
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

    /** 워치가 GPS를 조회/전송하는 권장 주기. 대상자별로 다를 필요가 없어 상수로 고정한다. */
    private static final int LIVE_LOCATION_INTERVAL_SECONDS = 10;

    /**
     * 앱이 expires_in_minutes를 안 보냈을 때 쓰는 자동 종료 시간.
     * 프론트 요청서의 예시값이 60분이고, 이미 배포된 앱은 enabled만 보내므로 그 요청도 무기한이 되지 않게 한다.
     */
    private static final int DEFAULT_TRACKING_MINUTES = 60;

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final WearDeviceRepository wearDeviceRepository;
    private final CareEventRecorder careEventRecorder;

    /** 보호자 모바일에서 워치 실제 연결 상태 조회. 연결된 적 없거나 해제된 상태면 null. */
    public WearDeviceStatusResponse getDeviceStatus(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        return findPrimaryCared(carer)
                .flatMap(wearDeviceRepository::findByCared)
                .filter(wearDevice -> !wearDevice.isDisconnected())
                .map(this::toStatusResponse)
                .orElse(null);
    }

    /** 보호자가 실시간 위치 공유를 켜거나 끈다. 아직 연결된 워치가 없으면 에러. */
    @Transactional
    public LiveLocationTrackingResponse setTracking(Integer carerId, LiveLocationTrackingRequest request) {
        WearDevice wearDevice = getPairedWearDeviceOrThrow(carerId);
        LocalDateTime now = LocalDateTime.now();

        Integer minutes = request.expiresInMinutes() == null ? DEFAULT_TRACKING_MINUTES : request.expiresInMinutes();
        LocalDateTime expiresAt = request.enabled() ? now.plusMinutes(minutes) : null;
        wearDevice.setLiveLocationEnabled(request.enabled(), expiresAt, now);

        careEventRecorder.record(
                wearDevice.getCared(),
                request.enabled() ? CareEventType.LIVE_LOCATION_STARTED : CareEventType.LIVE_LOCATION_STOPPED,
                now);

        return new LiveLocationTrackingResponse(
                wearDevice.isLiveLocationActive(now),
                DateTimes.toIsoString(wearDevice.getLiveLocationExpiresAt()),
                LIVE_LOCATION_INTERVAL_SECONDS,
                DateTimes.toIsoString(now));
    }

    /** 보호자 모바일에서 최신 위치 조회. 워치가 없거나 위치를 한 번도 안 보냈으면 null. */
    public LiveLocationResponse getLiveLocation(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        LocalDateTime now = LocalDateTime.now();
        return findPrimaryCared(carer)
                .flatMap(wearDeviceRepository::findByCared)
                .filter(wearDevice -> wearDevice.getLiveCapturedAt() != null)
                .map(wearDevice -> toLiveLocationResponse(wearDevice, now))
                .orElse(null);
    }

    /**
     * 워치가 최신 위치를 전송. 추적이 꺼져 있거나 만료됐으면 저장하지 않는다.
     * 워치가 상태 조회를 건너뛰고 위치만 계속 올리는 경우에도 만료가 실제로 지켜지도록 여기서도 검사한다.
     */
    @Transactional
    public LiveLocationUpdateResponse updateLiveLocation(Integer wearDeviceId, LiveLocationUpdateRequest request) {
        WearDevice wearDevice = wearDeviceRepository.getConnectedOrThrow(wearDeviceId);

        if (!wearDevice.isLiveLocationActive(LocalDateTime.now())) {
            return new LiveLocationUpdateResponse(false);
        }

        wearDevice.updateLiveLocation(
                request.latitude(), request.longitude(), request.accuracyMeters(),
                DateTimes.parseToKst(request.capturedAt()));
        wearDevice.touchLastSeen(LocalDateTime.now());
        return new LiveLocationUpdateResponse(true);
    }

    /** 워치에서 현재 추적을 계속해도 되는지 확인. */
    public LiveLocationTrackingStatusResponse getTrackingStatusForWear(Integer wearDeviceId) {
        WearDevice wearDevice = wearDeviceRepository.getConnectedOrThrow(wearDeviceId);
        return new LiveLocationTrackingStatusResponse(
                wearDevice.isLiveLocationActive(LocalDateTime.now()), LIVE_LOCATION_INTERVAL_SECONDS);
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
        String pairedAt = DateTimes.toIsoString(wearDevice.getConnectedAt());
        return new WearDeviceStatusResponse(
                wearDevice.getWearDeviceId(),
                true,
                wearDevice.getDeviceName(),
                pairedAt,
                pairedAt,
                DateTimes.toIsoString(wearDevice.getLastSeenAt()));
    }

    /** 만료 뒤에는 마지막 위치를 그대로 보여주되 "추적 중"으로는 표시하지 않는다. */
    private LiveLocationResponse toLiveLocationResponse(WearDevice wearDevice, LocalDateTime now) {
        return new LiveLocationResponse(
                wearDevice.isLiveLocationActive(now),
                wearDevice.getLiveLatitude(),
                wearDevice.getLiveLongitude(),
                wearDevice.getLiveAccuracyMeters(),
                DateTimes.toIsoString(wearDevice.getLiveCapturedAt()));
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
