package com.youngkke.careon.global.validation;

/** 여러 DTO에서 공통으로 쓰는 검증 정규식 모음. */
public final class ValidationPatterns {

    /** 영문+숫자 포함 8~20자 (특수문자 허용). */
    public static final String PASSWORD =
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]{8,20}$";

    /**
     * Expo 푸시 토큰 형식. 예: ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]
     * (Expo SDK 버전에 따라 ExpoPushToken[...] 형태도 나와서 둘 다 받는다.)
     * FCM/APNs 원시 토큰이 잘못 올라오는 걸 막기 위한 최소한의 형식 검사다.
     */
    public static final String EXPO_PUSH_TOKEN = "^Expo(nent)?PushToken\\[[^\\[\\]]+\\]$";

    private ValidationPatterns() {
    }
}
