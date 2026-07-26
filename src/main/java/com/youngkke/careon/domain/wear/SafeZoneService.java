package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.push.PushMessage;
import com.youngkke.careon.domain.push.PushSender;
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
import java.util.Map;
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
    private final PushSender pushSender;

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
            // 워치가 같은 이탈을 재시도한 경우. 이벤트도 푸시도 최초 1회만 나가야 하므로 여기서 끝낸다.
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

        boolean isNewEvent;
        try {
            event = safeZoneEventRepository.save(event);
            isNewEvent = true;
        } catch (DataIntegrityViolationException e) {
            // 같은 Idempotency-Key로 동시에 두 번 들어온 경우. 먼저 저장된 쪽에서 이미 푸시를 보냈다.
            event = safeZoneEventRepository
                    .findByWearDeviceAndIdempotencyKey(wearDevice, idempotencyKey)
                    .orElseThrow(() -> e);
            isNewEvent = false;
        }

        wearDevice.touchLastSeen(LocalDateTime.now());
        if (isNewEvent) {
            notifyCarer(event);
        }
        return toCreateResponse(event);
    }

    /**
     * 안심 구역 이탈도 SOS와 같은 긴급 알림이라, 보호자가 알림을 꺼둔 상태여도 푸시를 보낸다.
     * 푸시는 커밋 이후에 나가고 실패해도 예외를 삼키므로, 발송이 실패해도 이탈 이벤트 저장은 그대로 유지된다.
     */
    private void notifyCarer(SafeZoneEvent event) {
        Cared cared = event.getCared();
        Integer eventId = event.getSafeZoneEventId();

        pushSender.sendUrgentAfterCommit(
                cared.getCarer(),
                PushMessage.emergency(
                        "안심 구역 이탈이 감지됐어요",
                        "연결된 워치가 설정한 안심 구역을 벗어났어요.",
                        Map.of(
                                "type", "SAFE_ZONE_EXIT",
                                "event_id", eventId,
                                "cared_id", cared.getCaredId(),
                                "url", "/safe-zone-events/" + eventId)));
    }

    /**
     * 이벤트 ID로 이탈 1건 조회. 푸시 딥링크로 진입했을 때 쓴다.
     * 워치 사용자가 이미 응답했거나 뒤이어 SOS가 생긴 뒤에도 조회된다.
     * 남의 이벤트도 존재 여부를 흘리지 않도록 404로 답한다.
     */
    public ActiveSafeZoneEventResponse getEventById(Integer carerId, Integer eventId) {
        return safeZoneEventRepository
                .findById(eventId)
                .filter(event -> event.getCared().getCarer().getCarerId().equals(carerId))
                .map(this::toActiveResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAFE_ZONE_EVENT_NOT_FOUND));
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
