package com.youngkke.careon.domain.caretask.dto;

import com.youngkke.careon.domain.caretask.CareTaskKind;
import com.youngkke.careon.domain.caretask.CareTaskRepeat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/** 보호자 화면에서 보는 할 일 정의 1건. scheduledDate와 daysOfWeek는 반복 방식에 해당할 때만 값이 있다. */
public record CareTaskResponse(
        Integer taskId,
        String title,
        CareTaskKind kind,
        CareTaskRepeat repeatType,
        LocalTime scheduledTime,
        LocalDate scheduledDate,
        Set<DayOfWeek> daysOfWeek,
        boolean active) {}
