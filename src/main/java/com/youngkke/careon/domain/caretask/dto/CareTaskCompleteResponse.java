package com.youngkke.careon.domain.caretask.dto;

/** 체크/해제 결과. completed=false면 completedAt은 null이다. */
public record CareTaskCompleteResponse(Integer taskId, boolean completed, String completedAt) {}
