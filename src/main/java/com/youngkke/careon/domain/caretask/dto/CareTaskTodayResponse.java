package com.youngkke.careon.domain.caretask.dto;

import com.youngkke.careon.domain.caretask.CareTaskKind;

/**
 * 워치에 띄우는 오늘의 할 일 1건.
 *
 * @param scheduledAt 오늘 날짜에 예정 시각을 붙인 값. 워치가 날짜 계산을 하지 않아도 되게 완성된 일시로 준다.
 */
public record CareTaskTodayResponse(
        Integer taskId, String title, String scheduledAt, boolean completed, CareTaskKind kind) {}
