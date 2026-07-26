package com.youngkke.careon.domain.push;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.push.dto.PushTokenDeleteRequest;
import com.youngkke.careon.domain.push.dto.PushTokenRegisterRequest;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushTokenService {

    private static final String DEFAULT_PLATFORM = "expo";

    private final CarerRepository carerRepository;
    private final PushTokenRepository pushTokenRepository;

    /** 앱 로그인 후 발급받은 푸시 토큰을 등록한다. 같은 토큰을 여러 번 보내도 결과가 같도록 PUT(멱등)으로 둔다. */
    @Transactional
    public void register(Integer carerId, PushTokenRegisterRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        String platform = platformOrDefault(request.platform());

        pushTokenRepository
                .findByToken(request.token())
                .ifPresentOrElse(
                        existing -> existing.reassign(carer, platform),
                        () -> save(carer, request.token(), platform));
    }

    /** 로그아웃/알림 해제 시 토큰을 삭제한다. 없는 토큰이어도 그냥 성공으로 둔다 (재시도해도 같은 결과). */
    @Transactional
    public void unregister(Integer carerId, PushTokenDeleteRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        pushTokenRepository.deleteByCarerAndToken(carer, request.token());
    }

    private void save(Carer carer, String token, String platform) {
        try {
            pushTokenRepository.save(PushToken.builder()
                    .carer(carer)
                    .token(token)
                    .platform(platform)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 같은 토큰으로 동시에 두 번 등록 요청이 온 경우. 먼저 들어간 row의 소유자만 옮겨준다.
            pushTokenRepository
                    .findByToken(token)
                    .orElseThrow(() -> e)
                    .reassign(carer, platform);
        }
    }

    private String platformOrDefault(String platform) {
        return platform == null || platform.isBlank() ? DEFAULT_PLATFORM : platform;
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
