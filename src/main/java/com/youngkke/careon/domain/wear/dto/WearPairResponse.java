package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.carer.dto.CaredResponse;

/** 워치 페어링 완료 응답. */
public record WearPairResponse(String wearAccessToken, String wearRefreshToken, CaredResponse cared) {}
