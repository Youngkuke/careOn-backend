package com.youngkke.careon.domain.todo.dto;

/** 투두 체크 수정 응답. */
public record TodoCheckResponse(Integer todoId, Boolean isChecked, String message) {}
