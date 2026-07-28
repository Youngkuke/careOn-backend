package com.youngkke.careon.domain.wear.dto;

/** 워치 설정 화면에 보여줄 현재 연결 정보. */
public record WearConnectionResponse(
        Integer wearDeviceId,
        String deviceName,
        String pairedAt,
        CarerSummary carer) {

    /**
     * 워치에 표시할 보호자 정보.
     * 워치는 보호자 계정을 다루지 않으므로 화면에 쓰는 최소 항목만 내려보낸다. (Carer 엔티티를 그대로 노출하면
     * 비밀번호 해시·리프레시 토큰·진단 프로필까지 따라 나간다)
     */
    public record CarerSummary(Integer carerId, String name, String email) {}
}
