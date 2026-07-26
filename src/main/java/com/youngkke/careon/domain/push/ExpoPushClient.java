package com.youngkke.careon.domain.push;

import com.youngkke.careon.domain.push.dto.ExpoPushMessage;
import com.youngkke.careon.domain.push.dto.ExpoPushResponse;
import com.youngkke.careon.domain.push.dto.ExpoPushResponse.ExpoPushTicket;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Expo Push Service(exp.host) 호출만 담당한다.
 * 앱 전역 Jackson 설정(snake_case)이 섞이지 않도록 이 클래스 전용 RestClient를 직접 만들어 쓴다.
 */
@Component
public class ExpoPushClient {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushClient.class);

    /** Expo가 한 요청에 허용하는 메시지 개수 상한. */
    private static final int MAX_BATCH_SIZE = 100;

    private final RestClient restClient;
    private final String url;

    public ExpoPushClient(
            @Value("${expo.push.url:https://exp.host/--/api/v2/push/send}") String url,
            @Value("${expo.push.access-token:}") String accessToken) {
        this.url = url;

        RestClient.Builder builder = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        // Expo 대시보드에서 푸시 보안 강화를 켠 프로젝트만 access token이 필요하다.
        if (accessToken != null && !accessToken.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        this.restClient = builder.build();
    }

    /**
     * 같은 내용을 여러 토큰으로 보낸다.
     *
     * @return 더 이상 유효하지 않아 지워야 하는 토큰 목록
     */
    public List<String> send(List<String> tokens, PushMessage message) {
        List<String> expiredTokens = new ArrayList<>();
        for (int start = 0; start < tokens.size(); start += MAX_BATCH_SIZE) {
            List<String> batch = tokens.subList(start, Math.min(start + MAX_BATCH_SIZE, tokens.size()));
            expiredTokens.addAll(sendBatch(batch, message));
        }
        return expiredTokens;
    }

    private List<String> sendBatch(List<String> tokens, PushMessage message) {
        List<ExpoPushMessage> payload =
                tokens.stream().map(token -> ExpoPushMessage.of(token, message)).toList();

        ExpoPushResponse response = restClient
                .post()
                .uri(url)
                .body(payload)
                .retrieve()
                .body(ExpoPushResponse.class);

        if (response == null) {
            log.warn("Expo 푸시 응답이 비어 있습니다. 요청 {}건.", tokens.size());
            return List.of();
        }

        // 개별 건이 실패해도 HTTP는 200이라, ticket을 요청 순서대로 하나씩 맞춰본다.
        List<ExpoPushTicket> tickets = response.ticketsOrEmpty();
        List<String> expiredTokens = new ArrayList<>();
        for (int i = 0; i < tickets.size() && i < tokens.size(); i++) {
            ExpoPushTicket ticket = tickets.get(i);
            if (ticket.isOk()) {
                continue;
            }
            log.warn("Expo 푸시 거절. token={}, message={}", mask(tokens.get(i)), ticket.message());
            if (ticket.isDeviceNotRegistered()) {
                expiredTokens.add(tokens.get(i));
            }
        }
        return expiredTokens;
    }

    /** 토큰 전체는 로그에 남기지 않는다. */
    private String mask(String token) {
        return token.length() <= 12 ? "***" : token.substring(0, 12) + "...";
    }
}
