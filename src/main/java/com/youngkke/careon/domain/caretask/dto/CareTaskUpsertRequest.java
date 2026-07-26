package com.youngkke.careon.domain.caretask.dto;

import com.youngkke.careon.domain.caretask.CareTaskKind;
import com.youngkke.careon.domain.caretask.CareTaskRepeat;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * 보호자가 할 일을 등록·수정한다.
 *
 * <p>프론트 요청서에는 워치용 조회/체크만 있고 등록 API가 없었다. 등록 경로가 없으면 "오늘의 할 일"이 항상
 * 비어 있어 기능이 성립하지 않아 함께 만들었다.
 *
 * <p>생성 시에는 title·kind·repeatType·scheduledTime이 필요하고(서비스에서 검증), 수정 시에는 보낸 값만 반영한다.
 *
 * @param scheduledDate repeatType이 ONCE일 때만 쓴다. 나머지 반복에서는 무시한다.
 * @param daysOfWeek repeatType이 WEEKLY일 때만 쓴다. 예: ["MONDAY", "WEDNESDAY", "FRIDAY"]
 */
public record CareTaskUpsertRequest(
        @Size(max = 100, message = "값이 올바르지 않습니다.") String title,
        CareTaskKind kind,
        CareTaskRepeat repeatType,
        LocalTime scheduledTime,
        LocalDate scheduledDate,
        Set<DayOfWeek> daysOfWeek,
        Boolean active) {}
