package com.youngkke.careon.domain.document;

import com.youngkke.careon.domain.document.dto.DocumentHistoryCreateRequest;
import com.youngkke.careon.domain.document.dto.DocumentHistoryMutationResponse;
import com.youngkke.careon.domain.document.dto.DocumentHistoryResponse;
import com.youngkke.careon.domain.document.dto.DocumentHistoryUpdateRequest;
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

@RestController
@RequestMapping("/api/web/users/me/document-history")
@RequiredArgsConstructor
public class WebDocumentHistoryController {

    private final DocumentHistoryService documentHistoryService;

    @GetMapping
    public ResponseEntity<List<DocumentHistoryResponse>> getList(@CurrentCarerId Integer carerId) {
        return ResponseEntity.ok(documentHistoryService.getList(carerId));
    }

    @PostMapping
    public ResponseEntity<DocumentHistoryMutationResponse> create(
            @CurrentCarerId Integer carerId, @Valid @RequestBody DocumentHistoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentHistoryService.create(carerId, request));
    }

    @PatchMapping("/{historyId}")
    public ResponseEntity<DocumentHistoryMutationResponse> update(
            @CurrentCarerId Integer carerId,
            @PathVariable Integer historyId,
            @Valid @RequestBody DocumentHistoryUpdateRequest request) {
        return ResponseEntity.ok(documentHistoryService.update(carerId, historyId, request));
    }

    @DeleteMapping("/{historyId}")
    public ResponseEntity<MessageResponse> delete(
            @CurrentCarerId Integer carerId, @PathVariable Integer historyId) {
        return ResponseEntity.ok(documentHistoryService.delete(carerId, historyId));
    }
}
