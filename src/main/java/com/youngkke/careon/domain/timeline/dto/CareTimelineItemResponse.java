package com.youngkke.careon.domain.timeline.dto;

import com.youngkke.careon.domain.timeline.CareEventType;

/**
 * 타임라인 항목 1건.
 *
 * @param timelineId 화면에서 항목을 구분하는 키. 같은 SOS에서 생성·확인 두 항목이 나오므로 원본 이벤트 id가 아니라
 *     타임라인 기록 자체의 id로 만든다.
 * @param eventId 원본 이벤트 id(SOS·이탈). 눌러서 상세로 들어갈 때 쓴다. 대응하는 원본이 없으면 null.
 */
public record CareTimelineItemResponse(
        String timelineId, CareEventType type, String occurredAt, Integer eventId, String summary) {}
