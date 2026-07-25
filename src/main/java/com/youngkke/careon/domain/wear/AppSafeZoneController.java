package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.SafeZoneResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneUpsertRequest;
import com.youngkke.careon.global.auth.CurrentCarerId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/cared/{caredId}/safe-zone")
@RequiredArgsConstructor
public class AppSafeZoneController {

    private final SafeZoneService safeZoneService;

    /** 안심 구역 조회. 설정된 적 없으면 204. */
    @GetMapping
    public ResponseEntity<SafeZoneResponse> get(@CurrentCarerId Integer carerId, @PathVariable Integer caredId) {
        SafeZoneResponse response = safeZoneService.getForApp(carerId, caredId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    /** 안심 구역 설정 (없으면 생성, 있으면 갱신). */
    @PutMapping
    public ResponseEntity<SafeZoneResponse> upsert(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer caredId,
            @Valid @RequestBody SafeZoneUpsertRequest request) {
        return ResponseEntity.ok(safeZoneService.upsert(carerId, caredId, request));
    }
}
