package com.youngkke.careon.domain.wear;

import com.youngkke.careon.domain.wear.dto.HeartRateCreateRequest;
import com.youngkke.careon.domain.wear.dto.HeartRateCreateResponse;
import com.youngkke.careon.global.auth.CurrentWearDeviceId;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wear")
@RequiredArgsConstructor
public class WearHeartRateController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final HeartRateService heartRateService;

    /** 워치가 측정한 심박수를 저장한다. 재전송으로 같은 값이 중복 저장되지 않도록 Idempotency-Key를 요구한다. */
    @PostMapping("/heart-rates")
    public ResponseEntity<HeartRateCreateResponse> create(
            @CurrentWearDeviceId Integer wearDeviceId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody HeartRateCreateRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_MISSING);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(heartRateService.create(wearDeviceId, idempotencyKey, request));
    }
}
