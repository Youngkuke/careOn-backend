package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.HeartRateSource;

/** 보호자 모바일에서 보는 최신 심박수. 위험도 판정은 담지 않는다(§10). */
public record LatestHeartRateResponse(
        Integer heartRateId, Integer bpm, String measuredAt, HeartRateSource source, Integer wearDeviceId) {}
