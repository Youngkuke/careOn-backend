package com.youngkke.careon.domain.push.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.youngkke.careon.domain.push.PushMessage;
import java.util.Map;

/**
 * Expo Push Service로 그대로 나가는 요청 body 1건.
 * 이 프로젝트의 API JSON은 전부 snake_case지만 Expo는 camelCase(channelId)를 요구하므로,
 * 전용 RestClient(기본 컨버터)로 보내고 필드명도 @JsonProperty로 못 박아둔다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExpoPushMessage(
        String to,
        String title,
        String body,
        String sound,
        String priority,
        @JsonProperty("channelId") String channelId,
        Map<String, Object> data) {

    private static final String SOUND_DEFAULT = "default";

    public static ExpoPushMessage of(String token, PushMessage message) {
        return new ExpoPushMessage(
                token,
                message.title(),
                message.body(),
                SOUND_DEFAULT,
                message.priority(),
                message.channelId(),
                message.data());
    }
}
