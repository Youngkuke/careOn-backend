package com.youngkke.careon.domain.caretask;

import com.youngkke.careon.domain.caretask.dto.CareTaskResponse;
import com.youngkke.careon.domain.caretask.dto.CareTaskUpsertRequest;
import com.youngkke.careon.global.auth.CurrentCarerId;
import com.youngkke.careon.global.dto.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 보호자가 워치에 띄울 할 일을 관리하는 API.
 *
 * <p>프론트 요청서에는 워치용 조회/체크만 있었는데, 등록 경로가 없으면 "오늘의 할 일"이 항상 비어 있어
 * 기능이 성립하지 않는다. 계약을 확정할 때 경로·필드는 조정될 수 있다.
 */
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppCareTaskController {

    private final CareTaskService careTaskService;

    /** 등록된 할 일 전체 목록. 꺼둔 항목도 포함한다. */
    @GetMapping("/cared/{caredId}/care-tasks")
    public ResponseEntity<List<CareTaskResponse>> list(
            @CurrentCarerId Integer carerId, @PathVariable Integer caredId) {
        return ResponseEntity.ok(careTaskService.list(carerId, caredId));
    }

    /** 할 일 등록. */
    @PostMapping("/cared/{caredId}/care-tasks")
    public ResponseEntity<CareTaskResponse> create(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer caredId,
            @Valid @RequestBody CareTaskUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(careTaskService.create(carerId, caredId, request));
    }

    /** 할 일 부분 수정. 보낸 값만 반영한다. */
    @PatchMapping("/care-tasks/{taskId}")
    public ResponseEntity<CareTaskResponse> update(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer taskId,
            @Valid @RequestBody CareTaskUpsertRequest request) {
        return ResponseEntity.ok(careTaskService.update(carerId, taskId, request));
    }

    /** 할 일 삭제. 지금까지의 완료 기록은 남기고 목록에서만 내린다. */
    @DeleteMapping("/care-tasks/{taskId}")
    public ResponseEntity<MessageResponse> delete(@CurrentCarerId Integer carerId, @PathVariable Integer taskId) {
        careTaskService.delete(carerId, taskId);
        return ResponseEntity.ok(new MessageResponse("할 일을 삭제했어요."));
    }
}
