package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.push.PushMessage;
import com.youngkke.careon.domain.push.PushSender;
import com.youngkke.careon.domain.timeline.CareEventRecorder;
import com.youngkke.careon.domain.timeline.CareEventType;
import com.youngkke.careon.domain.wear.dto.ActiveSafeZoneEventResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventCreateRequest;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventCreateResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventLocationResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventRespondRequest;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventRespondResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneUpsertRequest;
import com.youngkke.careon.domain.wear.dto.WearActiveSafeZoneEventResponse;
import com.youngkke.careon.global.dto.CursorPageResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.Cursors;
import com.youngkke.careon.global.util.DateTimes;
import com.youngkke.careon.global.util.Pagination;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 안심 구역 설정과, 워치가 보고하는 이탈 이벤트를 함께 다룬다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafeZoneService {

    /**
     * 이탈 감지 후 워치 사용자가 응답할 수 있는 시간. 프론트 요청서의 예시(18:10:00 감지 → 18:10:30 마감)를 그대로 따랐다.
     * 이벤트마다 다를 이유가 없어 저장하지 않고 detectedAt에 더해 계산한다.
     */
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final WearDeviceRepository wearDeviceRepository;
    private final SafeZoneRepository safeZoneRepository;
    private final SafeZoneEventRepository safeZoneEventRepository;
    private final PushSender pushSender;
    private final CareEventRecorder careEventRecorder;

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

        // last_seen_at은 WearLastSeenInterceptor가 워치 요청 전체에 대해 갱신한다.
        if (isNewEvent) {
            careEventRecorder.record(
                    event.getCared(),
                    CareEventType.SAFE_ZONE_EXIT_DETECTED,
                    event.getDetectedAt(),
                    event.getSafeZoneEventId());
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
     * 이탈 알림에 응답이 없었음을 보호자에게 알린다.
     *
     * <p>괜찮다/도움이 필요하다는 본인이 의사를 밝힌 것이라 상황이 끝나지만, 무응답만 결말이 나지 않은 채로 남는다.
     * 못 보고 지나친 것인지 못 누르는 상황인지 서버는 구분할 수 없고, 그 둘은 겉으로 똑같아 보인다.
     * 이탈 자체를 이미 긴급으로 알린 건의 후속이라 같은 긴급 경로로 보낸다.
     *
     * <p>문구를 단정하지 않는 이유도 같다. 실제로 위급한지는 알 수 없고, 서버가 아는 건 "응답이 없다"는 사실뿐이다.
     */
    private void notifyNoResponse(SafeZoneEvent event) {
        Cared cared = event.getCared();
        Integer eventId = event.getSafeZoneEventId();

        pushSender.sendUrgentAfterCommit(
                cared.getCarer(),
                PushMessage.emergency(
                        "이탈 알림에 응답이 없어요",
                        "안심 구역을 벗어난 뒤 워치에서 응답이 없어요. 확인이 필요해요.",
                        Map.of(
                                "type", "SAFE_ZONE_NO_RESPONSE",
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

    /**
     * 안심 구역 이탈 이력 목록. 응답 완료된 건도 포함해 최신순으로 준다.
     * 권한 없는 cared를 요청하면 CARED_NOT_FOUND(404)라, 그 번호에 기록이 있는지 자체가 드러나지 않는다.
     */
    public CursorPageResponse<ActiveSafeZoneEventResponse> listHistory(
            Integer carerId, Integer caredId, String cursor, Integer limit) {
        Cared cared = getOwnedCaredOrThrow(carerId, caredId);
        int pageSize = Pagination.resolveLimit(limit);
        Cursors.Position position = Cursors.decode(cursor);

        // 다음 페이지가 있는지 판단하려고 한 건 더 읽는다. 별도 count 쿼리를 돌리지 않기 위해서다.
        Pageable pageable = Pageable.ofSize(pageSize + 1);
        List<SafeZoneEvent> found = position == null
                ? safeZoneEventRepository.findAllByCaredOrderByDetectedAtDescSafeZoneEventIdDesc(cared, pageable)
                : safeZoneEventRepository.findPageAfter(cared, position.timestamp(), position.id(), pageable);

        boolean hasNext = found.size() > pageSize;
        List<SafeZoneEvent> page = hasNext ? found.subList(0, pageSize) : found;
        String nextCursor = hasNext
                ? Cursors.encode(
                        page.get(page.size() - 1).getDetectedAt(),
                        page.get(page.size() - 1).getSafeZoneEventId())
                : null;

        return new CursorPageResponse<>(page.stream().map(this::toActiveResponse).toList(), nextCursor);
    }

    /**
     * 워치가 재시작된 뒤 진행 중인 이탈 화면을 복원한다. 응답할 수 있는 이벤트가 없으면 null.
     * 이미 응답했거나 마감이 지난 건은 복원할 화면이 없으므로 반환하지 않는다.
     */
    public WearActiveSafeZoneEventResponse getActiveEventForWear(Integer wearDeviceId) {
        WearDevice wearDevice = getWearDeviceOrThrow(wearDeviceId);
        LocalDateTime deadlineBoundary = LocalDateTime.now().minusSeconds(RESPONSE_TIMEOUT_SECONDS);
        return safeZoneEventRepository
                .findFirstByCaredAndResponseIsNullAndDetectedAtAfterOrderByDetectedAtDesc(
                        wearDevice.getCared(), deadlineBoundary)
                .map(this::toWearActiveResponse)
                .orElse(null);
    }

    /**
     * 응답 마감이 지나도록 워치에서 아무 응답이 없었던 이탈을 NO_RESPONSE로 확정한다.
     * 워치가 꺼지거나 배터리가 나가면 응답 API가 영영 안 들어오는데, 그대로 두면 그 이벤트가
     * 계속 "활성 이탈"로 조회되고 타임라인에도 결과가 안 남는다. 배치에서 확정해줘야 한다.
     */
    @Transactional
    public int confirmNoResponses() {
        LocalDateTime now = LocalDateTime.now();
        List<SafeZoneEvent> timedOut = safeZoneEventRepository.findAllByResponseIsNullAndDetectedAtBefore(
                now.minusSeconds(RESPONSE_TIMEOUT_SECONDS));
        timedOut.forEach(event -> {
            event.markNoResponse(now);
            careEventRecorder.record(
                    event.getCared(), CareEventType.SAFE_ZONE_NO_RESPONSE, now, event.getSafeZoneEventId());
            notifyNoResponse(event);
        });
        return timedOut.size();
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
        careEventRecorder.record(
                event.getCared(), toCareEventType(request.response()), now, event.getSafeZoneEventId());

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

    private CareEventType toCareEventType(SafeZoneResponseType response) {
        return switch (response) {
            case USER_OKAY -> CareEventType.SAFE_ZONE_USER_OKAY;
            case NEED_HELP -> CareEventType.SAFE_ZONE_NEED_HELP;
            case NO_RESPONSE -> CareEventType.SAFE_ZONE_NO_RESPONSE;
        };
    }

    private WearActiveSafeZoneEventResponse toWearActiveResponse(SafeZoneEvent event) {
        return new WearActiveSafeZoneEventResponse(
                event.getSafeZoneEventId(),
                event.getSafeZone().getSafeZoneId(),
                event.getStatus(),
                event.getResponse(),
                DateTimes.toIsoString(event.getDetectedAt()),
                DateTimes.toIsoString(event.getDetectedAt().plusSeconds(RESPONSE_TIMEOUT_SECONDS)),
                new SafeZoneEventLocationResponse(
                        event.getLatitude(),
                        event.getLongitude(),
                        event.getAccuracyMeters(),
                        DateTimes.toIsoString(event.getLocationCapturedAt())));
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
        return wearDeviceRepository.getConnectedOrThrow(wearDeviceId);
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
