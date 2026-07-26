package com.youngkke.careon.domain.push;

import java.util.Map;

/**
 * 보낼 푸시 1건의 내용. 받는 사람(토큰)은 PushSender가 채우므로 여기엔 없다.
 *
 * @param priority Expo 우선순위. null이면 Expo 기본값(default)을 쓴다.
 * @param channelId Android 알림 채널. null이면 앱 기본 채널로 간다.
 * @param data 앱이 알림을 탭했을 때 어디로 보낼지 판단하는 값들. 키는 앱과 맞춘 snake_case 그대로 쓴다.
 */
public record PushMessage(String title, String body, String priority, String channelId, Map<String, Object> data) {

    private static final String PRIORITY_HIGH = "high";

    /** 앱에서 긴급 알림 전용으로 만들어둔 Android 알림 채널. 앱에 이 채널이 없으면 기본 채널로 조용히 간다. */
    private static final String CHANNEL_EMERGENCY = "emergency";

    /** 일반 알림. 앱 기본 채널로 간다. */
    public static PushMessage normal(String title, String body, Map<String, Object> data) {
        return new PushMessage(title, body, null, null, data);
    }

    /** 긴급 알림(SOS·안심 구역 이탈). 잠금화면에서도 즉시 뜨도록 high 우선순위와 전용 채널을 지정한다. */
    public static PushMessage emergency(String title, String body, Map<String, Object> data) {
        return new PushMessage(title, body, PRIORITY_HIGH, CHANNEL_EMERGENCY, data);
    }
}
