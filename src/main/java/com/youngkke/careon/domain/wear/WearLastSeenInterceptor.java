package com.youngkke.careon.domain.wear;

import com.youngkke.careon.global.auth.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 워치 토큰으로 들어온 요청이 성공하면 last_seen_at을 갱신한다.
 *
 * <p>미통신 알림은 "워치에서 마지막으로 연락이 온 시각"으로 판단하는데, 그 연락에는 SOS 같은 쓰기 요청뿐
 * 아니라 안심 구역 조회나 오늘의 할 일 조회처럼 읽기만 하는 요청도 포함돼야 한다. 워치가 살아 있다는 신호는
 * 요청의 종류와 무관하기 때문이다. 서비스마다 따로 갱신하면 엔드포인트가 늘어날 때마다 빠뜨리게 되므로,
 * 워치 요청이 모두 지나가는 이 지점에서 한 번에 처리한다.
 */
@Component
@RequiredArgsConstructor
public class WearLastSeenInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final WearDeviceStatusService wearDeviceStatusService;

    /**
     * 응답을 다 보낸 뒤에 갱신한다. 요청 처리 중에 하면 원래 작업의 트랜잭션에 끼어들게 되고,
     * 이 갱신이 실패했다고 SOS 저장 같은 본래 작업이 되돌아가면 안 되기 때문이다.
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null || response.getStatus() >= 400) {
            return;
        }

        Integer wearDeviceId = resolveWearDeviceId(request);
        if (wearDeviceId == null) {
            return;
        }
        wearDeviceStatusService.recordLastSeen(wearDeviceId);
    }

    /** 토큰이 없거나 유효하지 않으면 null. 인증 실패 자체는 각 엔드포인트에서 이미 처리하므로 여기선 조용히 넘어간다. */
    private Integer resolveWearDeviceId(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        try {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            return jwtProvider.isWearAccessToken(token) ? jwtProvider.getWearDeviceId(token) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
