package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.ActiveSafeZoneEventResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneUpsertRequest;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.CursorPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/cared/{caredId}")
@RequiredArgsConstructor
public class AppSafeZoneController {

    private final SafeZoneService safeZoneService;

    /** 안심 구역 조회. 설정된 적 없으면 204. */
    @GetMapping("/safe-zone")
    public ResponseEntity<SafeZoneResponse> get(@CurrentCarerId Integer carerId, @PathVariable Integer caredId) {
        SafeZoneResponse response = safeZoneService.getForApp(carerId, caredId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    /** 안심 구역 설정 (없으면 생성, 있으면 갱신). */
    @PutMapping("/safe-zone")
    public ResponseEntity<SafeZoneResponse> upsert(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer caredId,
            @Valid @RequestBody SafeZoneUpsertRequest request) {
        return ResponseEntity.ok(safeZoneService.upsert(carerId, caredId, request));
    }

    /** 안심 구역 이탈 이력 목록. 최신순, next_cursor가 null이면 마지막 페이지. */
    @GetMapping("/safe-zone-events")
    public ResponseEntity<CursorPageResponse<ActiveSafeZoneEventResponse>> listHistory(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer caredId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(safeZoneService.listHistory(carerId, caredId, cursor, limit));
    }

    /** 보호자 모바일에서 현재 활성(워치 사용자 미응답) 이탈 이벤트 조회. 없으면 204. */
    @GetMapping("/safe-zone-events/active")
    public ResponseEntity<ActiveSafeZoneEventResponse> getActiveEvent(
            @CurrentCarerId Integer carerId, @PathVariable Integer caredId) {
        ActiveSafeZoneEventResponse response = safeZoneService.getActiveEventForApp(carerId, caredId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }
}
