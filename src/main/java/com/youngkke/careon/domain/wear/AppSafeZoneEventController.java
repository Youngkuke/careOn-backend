package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.ActiveSafeZoneEventResponse;
import com.youngkke.careon.global.auth.CurrentCarerId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 안심 구역 이탈 이벤트를 이벤트 ID만으로 다루는 API.
 * 돌봄 대상자별 조회는 cared_id가 경로에 들어가야 해서 AppSafeZoneController에 따로 있다.
 */
@RestController
@RequestMapping("/api/app/safe-zone-events")
@RequiredArgsConstructor
public class AppSafeZoneEventController {

    private final SafeZoneService safeZoneService;

    /**
     * 이벤트 ID로 이탈 1건 조회. 푸시 딥링크로 진입했을 때 쓴다.
     * 워치 사용자가 응답을 마친 뒤에도 조회되며, 남의 이벤트나 없는 이벤트는 모두 404다.
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<ActiveSafeZoneEventResponse> getById(
            @CurrentCarerId Integer carerId, @PathVariable Integer eventId) {
        return ResponseEntity.ok(safeZoneService.getEventById(carerId, eventId));
    }
}
