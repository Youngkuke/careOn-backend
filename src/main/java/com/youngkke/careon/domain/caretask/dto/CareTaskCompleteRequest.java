package com.youngkke.careon.domain.caretask.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 워치에서 할 일을 체크하거나 해제한다.
 *
 * @param completedAt 실제로 완료한 시각. 워치가 오프라인에서 체크한 뒤 나중에 올릴 수 있어 클라이언트 값을 받되,
 *     비어 있으면 서버 시각을 쓴다. completed=false면 무시한다.
 */
public record CareTaskCompleteRequest(
        @NotNull(message = "값이 누락되었습니다.") Boolean completed, String completedAt) {}
