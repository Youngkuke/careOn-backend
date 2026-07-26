package com.youngkke.careon.domain.wear.dto;

import com.youngkke.careon.domain.wear.HeartRateSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 워치가 올리는 심박수 측정값. caredId·wearDeviceId는 워치 토큰에서 서버가 식별하므로 body로 받지 않는다.
 *
 * @param bpm 허용 범위는 사람에게서 나올 수 있는 값의 바깥 경계로 잡았다. 이 범위를 벗어나면 센서 오류로 보고 저장하지 않는다.
 *     (서버는 위험도를 판정하지 않으므로, 정상/비정상 판단이 아니라 명백한 오측정만 거르는 용도다.)
 */
public record HeartRateCreateRequest(
        @NotNull(message = "값이 누락되었습니다.")
                @Min(value = 20, message = "값이 올바르지 않습니다.")
                @Max(value = 250, message = "값이 올바르지 않습니다.")
                Integer bpm,
        @NotBlank(message = "값이 누락되었습니다.") String measuredAt,
        @NotNull(message = "값이 누락되었습니다.") HeartRateSource source) {}
