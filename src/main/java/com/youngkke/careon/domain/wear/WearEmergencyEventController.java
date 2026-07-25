package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.EmergencyEventCreateRequest;
import com.youngkke.careon.domain.wear.dto.EmergencyEventCreateResponse;
import com.youngkke.careon.domain.wear.dto.EmergencyEventStatusResponse;
import com.youngkke.careon.global.auth.CurrentWearDeviceId;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wear/emergency-events")
@RequiredArgsConstructor
public class WearEmergencyEventController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final EmergencyEventService emergencyEventService;

    /** 워치가 SOS 발생 시 이벤트를 생성한다. */
    @PostMapping
    public ResponseEntity<EmergencyEventCreateResponse> create(
            @CurrentWearDeviceId Integer wearDeviceId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody EmergencyEventCreateRequest request) {
        requireIdempotencyKey(idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emergencyEventService.create(wearDeviceId, idempotencyKey, request));
    }

    /** 워치에서 보호자 확인 상태 polling. */
    @GetMapping("/{eventId}")
    public ResponseEntity<EmergencyEventStatusResponse> getStatus(
            @CurrentWearDeviceId Integer wearDeviceId, @PathVariable Integer eventId) {
        return ResponseEntity.ok(emergencyEventService.getStatusForWear(wearDeviceId, eventId));
    }

    private void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_MISSING);
        }
    }
}
