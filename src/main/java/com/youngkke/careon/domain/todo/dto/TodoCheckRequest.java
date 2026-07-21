package com.youngkke.careon.domain.todo.dto;

import jakarta.validation.constraints.NotNull;

public record TodoCheckRequest(@NotNull(message = "값이 누락되었습니다.") Boolean isChecked) {}
