package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.wear.dto.HeartRateCreateRequest;
import com.youngkke.careon.domain.wear.dto.HeartRateCreateResponse;
import com.youngkke.careon.domain.wear.dto.LatestHeartRateResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 워치가 올리는 심박수 저장과, 보호자 모바일의 최신값 조회를 담당한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HeartRateService {

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final WearDeviceRepository wearDeviceRepository;
    private final HeartRateRepository heartRateRepository;

    /** 워치가 측정값을 올린다. 동일 Idempotency-Key 요청은 기존 기록을 그대로 반환한다. */
    @Transactional
    public HeartRateCreateResponse create(
            Integer wearDeviceId, String idempotencyKey, HeartRateCreateRequest request) {
        WearDevice wearDevice = wearDeviceRepository.getConnectedOrThrow(wearDeviceId);

        Optional<HeartRate> existing = heartRateRepository.findByWearDeviceAndIdempotencyKey(wearDevice, idempotencyKey);
        if (existing.isPresent()) {
            // 워치가 같은 측정값을 재전송한 경우. 중복 저장 없이 여기서 끝낸다.
            return toCreateResponse(existing.get());
        }

        HeartRate heartRate = HeartRate.builder()
                .cared(wearDevice.getCared())
                .wearDevice(wearDevice)
                .idempotencyKey(idempotencyKey)
                .bpm(request.bpm())
                .measuredAt(DateTimes.parseToKstNotFuture(request.measuredAt()))
                .source(request.source())
                .build();

        try {
            heartRate = heartRateRepository.save(heartRate);
        } catch (DataIntegrityViolationException e) {
            // 같은 Idempotency-Key로 동시에 두 번 들어온 경우. 먼저 저장된 쪽을 그대로 쓴다.
            heartRate = heartRateRepository
                    .findByWearDeviceAndIdempotencyKey(wearDevice, idempotencyKey)
                    .orElseThrow(() -> e);
        }

        // last_seen_at은 WearLastSeenInterceptor가 워치 요청 전체에 대해 갱신한다.
        return toCreateResponse(heartRate);
    }

    /** 보호자 모바일에서 최신 심박수 조회. 측정 기록이 없으면 null. */
    public LatestHeartRateResponse getLatest(Integer carerId, Integer caredId) {
        Cared cared = getOwnedCaredOrThrow(carerId, caredId);
        return heartRateRepository
                .findFirstByCaredOrderByMeasuredAtDescHeartRateIdDesc(cared)
                .map(heartRate -> new LatestHeartRateResponse(
                        heartRate.getHeartRateId(),
                        heartRate.getBpm(),
                        DateTimes.toIsoString(heartRate.getMeasuredAt()),
                        heartRate.getSource(),
                        heartRate.getWearDevice().getWearDeviceId()))
                .orElse(null);
    }

    private HeartRateCreateResponse toCreateResponse(HeartRate heartRate) {
        return new HeartRateCreateResponse(
                heartRate.getHeartRateId(), heartRate.getBpm(), DateTimes.toIsoString(heartRate.getMeasuredAt()));
    }

    private Cared getOwnedCaredOrThrow(Integer carerId, Integer caredId) {
        Carer carer = carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return caredRepository
                .findByCaredIdAndCarer(caredId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARED_NOT_FOUND));
    }
}
