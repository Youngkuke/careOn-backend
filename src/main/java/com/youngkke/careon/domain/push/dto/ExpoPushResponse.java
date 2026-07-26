package com.youngkke.careon.domain.push.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Expo Push Service 응답. data는 요청에 넣은 메시지와 같은 순서로 1:1 대응한다.
 * 개별 건이 실패해도 HTTP 200으로 오기 때문에 ticket의 status를 따로 봐야 한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpoPushResponse(List<ExpoPushTicket> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExpoPushTicket(String status, String id, String message, Map<String, Object> details) {

        private static final String STATUS_OK = "ok";
        private static final String ERROR_DEVICE_NOT_REGISTERED = "DeviceNotRegistered";

        public boolean isOk() {
            return STATUS_OK.equals(status);
        }

        /** 앱 삭제/재설치 등으로 죽은 토큰. 계속 갖고 있어봐야 매번 실패하므로 지워야 한다. */
        public boolean isDeviceNotRegistered() {
            return details != null && ERROR_DEVICE_NOT_REGISTERED.equals(details.get("error"));
        }
    }

    public List<ExpoPushTicket> ticketsOrEmpty() {
        return data == null ? List.of() : data;
    }
}
