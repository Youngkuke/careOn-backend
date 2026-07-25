package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.SafeZoneResponseType;
import jakarta.validation.constraints.NotNull;

public record SafeZoneEventRespondRequest(@NotNull(message = "값이 누락되었습니다.") SafeZoneResponseType response) {}
