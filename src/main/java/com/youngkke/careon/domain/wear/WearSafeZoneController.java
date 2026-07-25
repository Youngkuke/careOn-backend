package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.SafeZoneEventCreateRequest;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventCreateResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventRespondRequest;
import com.youngkke.careon.domain.wear.dto.SafeZoneEventRespondResponse;
import com.youngkke.careon.domain.wear.dto.SafeZoneResponse;
import com.youngkke.careon.global.auth.CurrentWearDeviceId;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wear")
@RequiredArgsConstructor
public class WearSafeZoneController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final SafeZoneService safeZoneService;

    /** 워치가 현재 안심 구역을 조회. 없거나 비활성화 상태면 204. */
    @GetMapping("/safe-zone")
    public ResponseEntity<SafeZoneResponse> get(@CurrentWearDeviceId Integer wearDeviceId) {
        SafeZoneResponse response = safeZoneService.getForWear(wearDeviceId);
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    /** 워치가 안심 구역 이탈 이벤트를 생성한다. */
    @PostMapping("/safe-zone-events")
    public ResponseEntity<SafeZoneEventCreateResponse> createEvent(
            @CurrentWearDeviceId Integer wearDeviceId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody SafeZoneEventCreateRequest request) {
        requireIdempotencyKey(idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(safeZoneService.createEvent(wearDeviceId, idempotencyKey, request));
    }

    /** 워치 사용자의 이탈 응답. */
    @PatchMapping("/safe-zone-events/{eventId}/response")
    public ResponseEntity<SafeZoneEventRespondResponse> respond(
            @CurrentWearDeviceId Integer wearDeviceId,
            @PathVariable Integer eventId,
            @Valid @RequestBody SafeZoneEventRespondRequest request) {
        return ResponseEntity.ok(safeZoneService.respond(wearDeviceId, eventId, request));
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_MISSING);
        }
    }
}
