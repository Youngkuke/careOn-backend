package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.wear.dto.ActiveSafeZoneEventResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventCreateRequest;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventCreateResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventLocationResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventRespondRequest;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventRespondResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneUpsertRequest;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 안심 구역 설정과, 워치가 보고하는 이탈 이벤트를 함께 다룬다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafeZoneService {

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final WearDeviceRepository wearDeviceRepository;
    private final SafeZoneRepository safeZoneRepository;
    private final SafeZoneEventRepository safeZoneEventRepository;

    /** 보호자 모바일에서 안심 구역 조회. 설정된 적 없으면 null. */
    public SafeZoneResponse getForApp(Integer carerId, Integer caredId) {
        Cared cared = getOwnedCaredOrThrow(carerId, caredId);
        return safeZoneRepository.findByCared(cared).map(this::toResponse).orElse(null);
    }

    /** 보호자 모바일에서 안심 구역 설정 (없으면 생성, 있으면 갱신). */
    @Transactional
    public SafeZoneResponse upsert(Integer carerId, Integer caredId, SafeZoneUpsertRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        Cared cared = caredRepository
                .findByCaredIdAndCarer(caredId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARED_NOT_FOUND));

        SafeZone safeZone = safeZoneRepository
                .findByCared(cared)
                .map(existing -> {
                    existing.update(
                            request.name(),
                            request.latitude(),
                            request.longitude(),
                            request.radiusMeters(),
                            request.enabled());
                    return existing;
                })
                .orElseGet(() -> safeZoneRepository.save(SafeZone.builder()
                        .cared(cared)
                        .name(request.name())
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .radiusMeters(request.radiusMeters())
                        .enabled(request.enabled())
                        .createdByCarer(carer)
                        .build()));

        return toResponse(safeZone);
    }

    /** 워치가 현재 안심 구역을 조회. 없거나 비활성화 상태면 null. */
    public SafeZoneResponse getForWear(Integer wearDeviceId) {
        WearDevice wearDevice = getWearDeviceOrThrow(wearDeviceId);
        return safeZoneRepository
                .findByCared(wearDevice.getCared())
                .filter(SafeZone::isEnabled)
                .map(this::toResponse)
                .orElse(null);
    }

    /** 워치가 안심 구역 이탈을 감지해 이벤트를 생성한다. Idempotency-Key로 중복 생성을 막는다. */
    @Transactional
    public SafeZoneEventCreateResponse createEvent(
            Integer wearDeviceId, String idempotencyKey, SafeZoneEventCreateRequest request) {
        WearDevice wearDevice = getWearDeviceOrThrow(wearDeviceId);
        SafeZone safeZone = safeZoneRepository
                .findById(request.safeZoneId())
                .filter(zone -> zone.getCared().getCaredId().equals(wearDevice.getCared().getCaredId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.SAFE_ZONE_NOT_FOUND));

        Optional<SafeZoneEvent> existing =
                safeZoneEventRepository.findByWearDeviceAndIdempotencyKey(wearDevice, idempotencyKey);
        if (existing.isPresent()) {
            return toCreateResponse(existing.get());
        }

        SafeZoneEvent event = SafeZoneEvent.builder()
                .safeZone(safeZone)
                .cared(wearDevice.getCared())
                .wearDevice(wearDevice)
                .idempotencyKey(idempotencyKey)
                .status(request.status())
                .latitude(request.location().latitude())
                .longitude(request.location().longitude())
                .accuracyMeters(request.location().accuracyMeters())
                .locationCapturedAt(DateTimes.parseToKst(request.location().capturedAt()))
                .detectedAt(DateTimes.parseToKst(request.detectedAt()))
                .build();

        try {
            event = safeZoneEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            event = safeZoneEventRepository
                    .findByWearDeviceAndIdempotencyKey(wearDevice, idempotencyKey)
                    .orElseThrow(() -> e);
        }

        wearDevice.touchLastSeen(LocalDateTime.now());
        return toCreateResponse(event);
    }

    /** 보호자 모바일에서 현재 활성(워치 사용자 미응답) 이탈 이벤트 조회. 없으면 null. */
    public ActiveSafeZoneEventResponse getActiveEventForApp(Integer carerId, Integer caredId) {
        Cared cared = getOwnedCaredOrThrow(carerId, caredId);
        return safeZoneEventRepository
                .findFirstByCaredAndResponseIsNullOrderByDetectedAtDesc(cared)
                .map(this::toActiveResponse)
                .orElse(null);
    }

    /** 워치 사용자(돌봄 대상자)의 이탈 응답. */
    @Transactional
    public SafeZoneEventRespondResponse respond(
            Integer wearDeviceId, Integer eventId, SafeZoneEventRespondRequest request) {
        WearDevice wearDevice = getWearDeviceOrThrow(wearDeviceId);
        SafeZoneEvent event = safeZoneEventRepository
                .findBySafeZoneEventIdAndWearDevice(eventId, wearDevice)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAFE_ZONE_EVENT_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        event.respond(request.response(), now);

        return new SafeZoneEventRespondResponse(
                event.getSafeZoneEventId(), event.getResponse(), DateTimes.toIsoString(now));
    }

    private SafeZoneEventCreateResponse toCreateResponse(SafeZoneEvent event) {
        return new SafeZoneEventCreateResponse(event.getSafeZoneEventId(), event.getStatus());
    }

    private ActiveSafeZoneEventResponse toActiveResponse(SafeZoneEvent event) {
        SafeZoneEventLocationResponse location = new SafeZoneEventLocationResponse(
                event.getLatitude(),
                event.getLongitude(),
                event.getAccuracyMeters(),
                DateTimes.toIsoString(event.getLocationCapturedAt()));

        return new ActiveSafeZoneEventResponse(
                event.getSafeZoneEventId(),
                event.getSafeZone().getSafeZoneId(),
                event.getCared().getCaredId(),
                event.getStatus(),
                event.getResponse(),
                location,
                DateTimes.toIsoString(event.getDetectedAt()),
                DateTimes.toIsoString(event.getRespondedAt()));
    }

    private SafeZoneResponse toResponse(SafeZone safeZone) {
        return new SafeZoneResponse(
                safeZone.getSafeZoneId(),
                safeZone.getName(),
                safeZone.getLatitude(),
                safeZone.getLongitude(),
                safeZone.getRadiusMeters(),
                safeZone.isEnabled());
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
