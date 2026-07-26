package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.carer.dto.CaredResponse;
import com.youngkke.careon.domain.timeline.CareEventRecorder;
import com.youngkke.careon.domain.timeline.CareEventType;
import com.youngkke.careon.domain.wear.dto.WearPairRequest;
import com.youngkke.careon.domain.wear.dto.WearPairResponse;
import com.youngkke.careon.domain.wear.dto.WearPairingCodeResponse;
import com.youngkke.careon.domain.wear.dto.WearRefreshRequest;
import com.youngkke.careon.domain.wear.dto.WearRefreshResponse;
import com.youngkke.careon.global.auth.JwtProvider;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 워치 연결 코드 발급, 페어링, access token 재발급을 담당한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WearAuthService {

    /** 연결 코드 유효 시간. 짧게 잡아도 보호자가 워치에 바로 입력하는 흐름이라 문제없다. */
    private static final long PAIRING_CODE_VALIDITY_SECONDS = 300;
    private static final int PAIRING_CODE_MAX_ATTEMPTS = 5;
    private static final String DEFAULT_DEVICE_NAME = "CareOn 워치";

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final WearDeviceRepository wearDeviceRepository;
    private final WearPairingCodeRepository wearPairingCodeRepository;
    private final JwtProvider jwtProvider;
    private final CareEventRecorder careEventRecorder;

    /** 보호자 모바일에서 워치 연결 코드 발급 (cared_id를 이미 아는 경우). */
    @Transactional
    public WearPairingCodeResponse issuePairingCode(Integer carerId, Integer caredId) {
        Carer carer = getCarerOrThrow(carerId);
        Cared cared = caredRepository
                .findByCaredIdAndCarer(caredId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARED_NOT_FOUND));
        return issuePairingCodeFor(carer, cared);
    }

    /**
     * 돌봄 대상자 등록 화면 없이 워치 연결만 쓰는 앱을 위한 발급.
     * 보호자에게 연결된 cared가 없으면 내부적으로 1개 자동 생성하고, 있으면 그중 가장 먼저 만든 걸 재사용한다.
     */
    @Transactional
    public WearPairingCodeResponse issuePairingCodeAutoProvision(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        Cared cared = caredRepository.findAllByCarerOrderByCaredIdAsc(carer).stream()
                .findFirst()
                .orElseGet(() -> caredRepository.save(Cared.builder().carer(carer).build()));
        return issuePairingCodeFor(carer, cared);
    }

    private WearPairingCodeResponse issuePairingCodeFor(Carer carer, Cared cared) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(PAIRING_CODE_VALIDITY_SECONDS);

        String code = generateUnusedCode(now);
        wearPairingCodeRepository.save(WearPairingCode.builder()
                .cared(cared)
                .codeHash(sha256(code))
                .expiresAt(expiresAt)
                .createdByCarer(carer)
                .build());

        return new WearPairingCodeResponse(code, DateTimes.toIsoString(expiresAt));
    }

    /** 워치에서 6자리 연결 코드를 입력해 페어링을 완료한다. */
    @Transactional
    public WearPairResponse pair(WearPairRequest request) {
        LocalDateTime now = LocalDateTime.now();
        WearPairingCode pairingCode = wearPairingCodeRepository
                .findFirstByCodeHashAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        sha256(request.code()), now)
                .orElseThrow(() -> new BusinessException(ErrorCode.WEAR_PAIRING_CODE_INVALID));
        pairingCode.markUsed(now);

        Cared cared = pairingCode.getCared();
        String deviceName = resolveDeviceName(request.deviceName());

        // 재페어링: 기존에 연결된 워치가 있으면 같은 row를 재사용한다 (기존 refreshToken은 아래에서 자동 회전되어 무효화됨).
        // 새 row로 교체(삭제 후 삽입)하면 이미 쌓인 emergency_event/safe_zone_event의 wear_device_id FK가 깨진다.
        WearDevice wearDevice = wearDeviceRepository
                .findByCared(cared)
                .map(existing -> {
                    existing.reconnect(deviceName, now);
                    return existing;
                })
                .orElseGet(() -> wearDeviceRepository.save(WearDevice.builder()
                        .cared(cared)
                        .deviceName(deviceName)
                        .refreshTokenHash("") // 아래에서 실제 refreshToken 발급 후 회전
                        .connectedAt(now)
                        .lastSeenAt(now)
                        .build()));

        String accessToken = jwtProvider.createWearAccessToken(wearDevice.getWearDeviceId());
        String refreshToken = jwtProvider.createWearRefreshToken(wearDevice.getWearDeviceId());
        // BCrypt는 72바이트 넘는 입력에서 예외를 던지는데 JWT는 항상 그보다 길다. refreshToken은 이미
        // 고 엔트로피 랜덤값이라 브루트포스 방어용 salt/slow-hash가 필요 없어서 SHA-256으로 충분하다.
        wearDevice.rotateRefreshToken(sha256(refreshToken));
        careEventRecorder.record(cared, CareEventType.WEAR_PAIRED, now);

        return new WearPairResponse(
                wearDevice.getWearDeviceId(),
                accessToken,
                refreshToken,
                jwtProvider.getWearAccessTokenValiditySeconds(),
                toCaredResponse(cared));
    }

    /** 워치 access token 갱신. refreshToken도 함께 회전시킨다. */
    @Transactional
    public WearRefreshResponse refresh(WearRefreshRequest request) {
        String refreshToken = request.wearRefreshToken();
        Integer wearDeviceId;
        try {
            if (!jwtProvider.isWearRefreshToken(refreshToken)) {
                throw new BusinessException(ErrorCode.WEAR_REFRESH_TOKEN_INVALID);
            }
            wearDeviceId = jwtProvider.getWearDeviceId(refreshToken);
        } catch (JwtException e) {
            throw new BusinessException(ErrorCode.WEAR_REFRESH_TOKEN_INVALID);
        }

        WearDevice wearDevice = wearDeviceRepository
                .findById(wearDeviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WEAR_REFRESH_TOKEN_INVALID));
        if (wearDevice.isDisconnected()) {
            // 해제 시 refreshTokenHash를 비우므로 아래 비교에서도 걸리지만, 워치가 "재인증하면 되겠다"고
            // 오해하지 않도록 해제됐다는 사실을 명시적으로 알려준다.
            throw new BusinessException(ErrorCode.WEAR_DEVICE_DISCONNECTED);
        }
        if (!sha256(refreshToken).equals(wearDevice.getRefreshTokenHash())) {
            throw new BusinessException(ErrorCode.WEAR_REFRESH_TOKEN_INVALID);
        }

        String newAccessToken = jwtProvider.createWearAccessToken(wearDeviceId);
        String newRefreshToken = jwtProvider.createWearRefreshToken(wearDeviceId);
        wearDevice.rotateRefreshToken(sha256(newRefreshToken));
        wearDevice.touchLastSeen(LocalDateTime.now());

        return new WearRefreshResponse(
                newAccessToken, newRefreshToken, jwtProvider.getWearAccessTokenValiditySeconds());
    }

    /**
     * 보호자가 워치 연결을 해제한다. 이미 해제됐거나 연결된 워치가 아예 없어도 성공으로 답한다.
     * 사용자 입장에서 "해제" 버튼을 두 번 누르거나 응답을 못 받고 재시도하는 건 모두 같은 의도이고,
     * 그때 에러를 주면 이미 원하는 상태(해제됨)인데도 실패한 것처럼 보이기 때문이다.
     */
    @Transactional
    public void disconnect(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        LocalDateTime now = LocalDateTime.now();
        caredRepository.findAllByCarerOrderByCaredIdAsc(carer).stream()
                .findFirst()
                .flatMap(wearDeviceRepository::findByCared)
                .filter(wearDevice -> !wearDevice.isDisconnected())
                .ifPresent(wearDevice -> {
                    wearDevice.disconnect(now);
                    careEventRecorder.record(wearDevice.getCared(), CareEventType.WEAR_UNPAIRED, now);
                });
    }

    /** 워치가 표시명을 안 보내주는 동안 쓸 기본값. 보호자 화면에 기기 ID가 그대로 노출되는 걸 막는 게 목적이다. */
    private String resolveDeviceName(String requestedDeviceName) {
        return requestedDeviceName == null || requestedDeviceName.isBlank()
                ? DEFAULT_DEVICE_NAME
                : requestedDeviceName.trim();
    }

    private String generateUnusedCode(LocalDateTime now) {
        SecureRandom random = new SecureRandom();
        for (int attempt = 0; attempt < PAIRING_CODE_MAX_ATTEMPTS; attempt++) {
            String code = String.format("%06d", random.nextInt(1_000_000));
            if (!wearPairingCodeRepository.existsByCodeHashAndUsedAtIsNullAndExpiresAtAfter(sha256(code), now)) {
                return code;
            }
        }
        throw new IllegalStateException("연결 코드 생성에 반복적으로 실패했습니다.");
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private CaredResponse toCaredResponse(Cared cared) {
        return new CaredResponse(
                cared.getCaredId(),
                cared.getCaredRelation(),
                cared.getAge(),
                cared.getConditionSummary(),
                cared.getSeverityLevel());
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
