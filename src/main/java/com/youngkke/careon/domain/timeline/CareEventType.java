package com.youngkke.careon.domain.timeline;

/**
 * 타임라인에 표시되는 사건 종류.
 *
 * <p>summary는 저장 시점에 확정해 care_event에 함께 적는다. 나중에 문구를 바꿔도 과거 기록의 표현이 그대로
 * 유지돼야 하고, 조회할 때마다 원본 테이블을 다시 읽어 문장을 만들지 않기 위해서다.
 *
 * <p>source는 timeline_id의 접두어다. 같은 SOS 1건에서 생성·확인 두 사건이 나오므로, timeline_id는
 * 원본 이벤트 id가 아니라 care_event의 id로 만든다. (원본 id로 만들면 두 사건의 id가 겹친다.)
 */
public enum CareEventType {
    EMERGENCY_CREATED("emergency", "도움 요청이 왔어요."),
    EMERGENCY_ACKNOWLEDGED("emergency", "보호자가 SOS를 확인했어요."),
    SAFE_ZONE_EXIT_DETECTED("safe-zone", "안심 구역을 벗어났어요."),
    SAFE_ZONE_USER_OKAY("safe-zone", "괜찮다고 응답했어요."),
    SAFE_ZONE_NEED_HELP("safe-zone", "도움이 필요하다고 응답했어요."),
    SAFE_ZONE_NO_RESPONSE("safe-zone", "이탈 알림에 응답이 없었어요."),
    WEAR_PAIRED("wear", "워치를 연결했어요."),
    WEAR_UNPAIRED("wear", "워치 연결을 해제했어요."),
    LIVE_LOCATION_STARTED("live-location", "실시간 위치 공유를 시작했어요."),
    LIVE_LOCATION_STOPPED("live-location", "실시간 위치 공유를 중지했어요.");

    private final String source;
    private final String summary;

    CareEventType(String source, String summary) {
        this.source = source;
        this.summary = summary;
    }

    public String getSource() {
        return source;
    }

    public String getSummary() {
        return summary;
    }
}
