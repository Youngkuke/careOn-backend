package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.wear.dto.ActiveEmergencyEventResponse;
import com.youngkke.careon.domain.wear.dto.EmergencyEventAcknowledgeResponse;
import com.youngkke.careon.domain.wear.dto.EmergencyEventCreateRequest;
import com.youngkke.careon.domain.wear.dto.EmergencyEventCreateResponse;
import com.youngkke.careon.domain.wear.dto.EmergencyEventStatusResponse;
import com.youngkke.careon.domain.wear.dto.EmergencyLocationRequest;
import com.youngkke.careon.domain.wear.dto.LocationPointResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmergencyEventService {

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final WearDeviceRepository wearDeviceRepository;
    private final EmergencyEventRepository emergencyEventRepository;

    /** 워치가 SOS 발생 시 이벤트를 생성한다. 동일 Idempotency-Key 요청은 기존 이벤트를 그대로 반환한다. */
    @Transactional
    public EmergencyEventCreateResponse create(
            Integer wearDeviceId, String idempotencyKey, EmergencyEventCreateRequest request) {
        WearDevice wearDevice = getWearDeviceOrThrow(wearDeviceId);
        validateLocationConsistency(request.locationStatus(), request.location());

        Optional<EmergencyEvent> existing =
                emergencyEventRepository.findByWearDeviceAndIdempotencyKey(wearDevice, idempotencyKey);
        if (existing.isPresent()) {
            return toCreateResponse(existing.get());
        }

        EmergencyLocationRequest location = request.location();
        EmergencyEvent event = EmergencyEvent.builder()
                .cared(wearDevice.getCared())
                .wearDevice(wearDevice)
                .idempotencyKey(idempotencyKey)
                .trigger(request.trigger())
                .heartRateBpm(request.heartRateBpm())
                .latitude(location == null ? null : location.latitude())
                .longitude(location == null ? null : location.longitude())
                .locationAccuracyM(location == null ? null : location.accuracyMeters())
                .locationCapturedAt(location == null ? null : DateTimes.parseToKst(location.capturedAt()))
                .locationSource(location == null ? null : location.source())
                .locationStatus(request.locationStatus())
                .requestedAt(DateTimes.parseToKst(request.requestedAt()))
                .build();

        try {
            event = emergencyEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            event = emergencyEventRepository
                    .findByWearDeviceAndIdempotencyKey(wearDevice, idempotencyKey)
                    .orElseThrow(() -> e);
        }

        wearDevice.touchLastSeen(LocalDateTime.now());
        return toCreateResponse(event);
    }

    /** 워치에서 보호자 확인 상태 polling. */
    public EmergencyEventStatusResponse getStatusForWear(Integer wearDeviceId, Integer eventId) {
        WearDevice wearDevice = getWearDeviceOrThrow(wearDeviceId);
        EmergencyEvent event = emergencyEventRepository
                .findByEmergencyEventIdAndCared(eventId, wearDevice.getCared())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMERGENCY_EVENT_NOT_FOUND));
        return new EmergencyEventStatusResponse(event.getEmergencyEventId(), event.getStatus());
    }

    /** 보호자 모바일에서 현재 활성(가장 최근 미확인) SOS 조회. 없으면 null. */
    public ActiveEmergencyEventResponse getActive(Integer carerId, Integer caredId) {
        Cared cared = getOwnedCaredOrThrow(carerId, caredId);
        return emergencyEventRepository
                .findFirstByCaredAndStatusOrderByRequestedAtDesc(cared, EmergencyStatus.PENDING)
                .map(this::toActiveResponse)
                .orElse(null);
    }

    /**
     * 보호자가 "확인했어요" 처리. 같은 대상자에 대해 그 사이 쌓인 다른 미확인 건들도 하나의 SOS 상황으로 보고 함께 확인 처리한다.
     */
    @Transactional
    public EmergencyEventAcknowledgeResponse acknowledge(Integer carerId, Integer eventId) {
        Carer carer = getCarerOrThrow(carerId);
        EmergencyEvent event = emergencyEventRepository
                .findById(eventId)
                .filter(e -> e.getCared().getCarer().getCarerId().equals(carerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.EMERGENCY_EVENT_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        event.acknowledge(carer, now);

        List<EmergencyEvent> otherPending =
                emergencyEventRepository.findAllByCaredAndStatus(event.getCared(), EmergencyStatus.PENDING);
        otherPending.forEach(other -> other.acknowledge(carer, now));

        return new EmergencyEventAcknowledgeResponse(
                event.getEmergencyEventId(), event.getStatus(), DateTimes.toIsoString(now), carerId);
    }

    private void validateLocationConsistency(LocationStatus locationStatus, EmergencyLocationRequest location) {
        boolean shouldHaveLocation =
                locationStatus == LocationStatus.CURRENT || locationStatus == LocationStatus.LAST_KNOWN;
        if (shouldHaveLocation && location == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!shouldHaveLocation && location != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private EmergencyEventCreateResponse toCreateResponse(EmergencyEvent event) {
        return new EmergencyEventCreateResponse(
                event.getEmergencyEventId(), event.getStatus(), DateTimes.toIsoString(event.getRequestedAt()));
    }

    private ActiveEmergencyEventResponse toActiveResponse(EmergencyEvent event) {
        LocationPointResponse location = event.getLatitude() == null
                ? null
                : new LocationPointResponse(
                        event.getLatitude(),
                        event.getLongitude(),
                        event.getLocationAccuracyM(),
                        DateTimes.toIsoString(event.getLocationCapturedAt()),
                        event.getLocationSource());

        return new ActiveEmergencyEventResponse(
                event.getEmergencyEventId(),
                event.getCared().getCaredId(),
                event.getTrigger(),
                event.getHeartRateBpm(),
                event.getStatus(),
                location,
                event.getLocationStatus(),
                DateTimes.toIsoString(event.getRequestedAt()));
    }

    private WearDevice getWearDeviceOrThrow(Integer wearDeviceId) {
        return wearDeviceRepository
                .findById(wearDeviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WEAR_DEVICE_NOT_FOUND));
    }

    private Cared getOwnedCaredOrThrow(Integer carerId, Integer caredId) {
        Carer carer = getCarerOrThrow(carerId);
        return caredRepository
                .findByCaredIdAndCarer(caredId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARED_NOT_FOUND));
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
