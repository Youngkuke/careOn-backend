package com.youngkke.careon.domain.wear;

/** 긴급 이벤트 생성 시점의 위치 확보 결과. */
public enum LocationStatus {
    CURRENT,
    LAST_KNOWN,
    UNAVAILABLE,
    PERMISSION_DENIED,
    GPS_DISABLED
}
