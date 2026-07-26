package com.youngkke.careon.domain.wear.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * 앱·워치가 이름으로 직접 읽는 필드가 실제로 그 이름으로 나가는지 확인한다.
 *
 * <p>boolean을 담은 record 필드는 접근자 이름이 isXxx()라서, Jackson이 "is"를 접두어로 보고 떼어버리면
 * is_tracking이 아니라 tracking으로 나갈 수 있다. 프론트가 is_tracking을 정식 필드로 쓰겠다고 했으므로
 * 추측하지 말고 직렬화 결과로 못 박아둔다.
 */
@SpringBootTest
class WearJsonFieldNameTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 최신_위치_응답은_is_tracking으로_나간다() {
        String json = objectMapper.writeValueAsString(
                new LiveLocationResponse(true, 37.4965, 126.9572, 18.0, "2026-07-27T10:00:00+09:00"));

        assertTrue(json.contains("\"is_tracking\":true"), "실제 직렬화 결과: " + json);
        assertTrue(json.contains("\"accuracy_meters\":18.0"), "실제 직렬화 결과: " + json);
        assertTrue(json.contains("\"captured_at\":"), "실제 직렬화 결과: " + json);
    }

    @Test
    void 추적_설정_응답은_expires_at과_interval_seconds를_포함한다() {
        String json = objectMapper.writeValueAsString(new LiveLocationTrackingResponse(
                true, "2026-07-27T11:00:00+09:00", 10, "2026-07-27T10:00:00+09:00"));

        assertTrue(json.contains("\"enabled\":true"), "실제 직렬화 결과: " + json);
        assertTrue(json.contains("\"expires_at\":"), "실제 직렬화 결과: " + json);
        assertTrue(json.contains("\"interval_seconds\":10"), "실제 직렬화 결과: " + json);
    }

    @Test
    void 워치_연결_상태_응답은_device_name과_paired_at을_포함한다() {
        String json = objectMapper.writeValueAsString(new WearDeviceStatusResponse(
                3,
                true,
                "Wear OS Small Round",
                "2026-07-26T18:00:00+09:00",
                "2026-07-26T18:00:00+09:00",
                "2026-07-26T18:05:00+09:00"));

        assertTrue(json.contains("\"wear_device_id\":3"), "실제 직렬화 결과: " + json);
        assertTrue(json.contains("\"connected\":true"), "실제 직렬화 결과: " + json);
        assertTrue(json.contains("\"device_name\":"), "실제 직렬화 결과: " + json);
        assertTrue(json.contains("\"paired_at\":"), "실제 직렬화 결과: " + json);
        assertTrue(json.contains("\"last_seen_at\":"), "실제 직렬화 결과: " + json);
    }

    @Test
    void 기기_상태_보고_응답은_low_battery_notified로_나간다() {
        String json = objectMapper.writeValueAsString(new WearDeviceStatusReportResponse(24, false));

        assertTrue(json.contains("\"battery_percent\":24"), "실제 직렬화 결과: " + json);
        assertTrue(json.contains("\"low_battery_notified\":false"), "실제 직렬화 결과: " + json);
    }
}
