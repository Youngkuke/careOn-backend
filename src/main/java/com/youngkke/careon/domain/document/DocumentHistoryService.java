package com.youngkke.careon.domain.document;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.document.dto.DocumentHistoryCreateRequest;
import com.youngkke.careon.domain.document.dto.DocumentHistoryMutationResponse;
import com.youngkke.careon.domain.document.dto.DocumentHistoryResponse;
import com.youngkke.careon.domain.document.dto.DocumentHistoryUpdateRequest;
import com.youngkke.careon.domain.policy.Policy;
import com.youngkke.careon.domain.policy.PolicyRepository;
import com.youngkke.careon.global.dto.MessageResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저의 서류 이력 관리. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentHistoryService {

    private final UserDocumentHistoryRepository userDocumentHistoryRepository;
    private final DocumentRepository documentRepository;
    private final PolicyRepository policyRepository;
    private final CarerRepository carerRepository;

    /** 내 서류 이력 조회. */
    public List<DocumentHistoryResponse> getList(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        return userDocumentHistoryRepository.findAllWithDetailByCarer(carer).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 서류 이력 저장. */
    @Transactional
    public DocumentHistoryMutationResponse create(Integer carerId, DocumentHistoryCreateRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        Document document = documentRepository.findById(request.documentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));
        Policy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        UserDocumentHistory history = userDocumentHistoryRepository.save(UserDocumentHistory.builder()
                .carer(carer)
                .document(document)
                .policy(policy)
                .issuedDate(parseDateTime(request.issuedDate()))
                .validUntil(request.validUntil())
                .directUtter(request.directUtter())
                .confirmedByUser(request.confirmedByUser())
                .build());

        return new DocumentHistoryMutationResponse(history.getHistoryId(), "서류 이력이 저장되었습니다.");
    }

    /** 서류 이력 수정. */
    @Transactional
    public DocumentHistoryMutationResponse update(
            Integer carerId, Integer historyId, DocumentHistoryUpdateRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        UserDocumentHistory history = userDocumentHistoryRepository.findByHistoryIdAndCarer(historyId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_HISTORY_NOT_FOUND));

        history.update(
                parseDateTime(request.issuedDate()),
                request.validUntil(),
                request.directUtter(),
                request.confirmedByUser());

        return new DocumentHistoryMutationResponse(history.getHistoryId(), "서류 이력이 수정되었습니다.");
    }

    /** 서류 이력 삭제. */
    @Transactional
    public MessageResponse delete(Integer carerId, Integer historyId) {
        Carer carer = getCarerOrThrow(carerId);
        UserDocumentHistory history = userDocumentHistoryRepository.findByHistoryIdAndCarer(historyId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_HISTORY_NOT_FOUND));

        userDocumentHistoryRepository.delete(history);

        return new MessageResponse("서류 이력이 삭제되었습니다.");
    }

    private DocumentHistoryResponse toResponse(UserDocumentHistory history) {
        return new DocumentHistoryResponse(
                history.getHistoryId(),
                history.getCarer().getCarerId(),
                history.getDocument().getDocumentId(),
                history.getDocument().getDocumentName(),
                history.getPolicy().getPolicyId(),
                history.getPolicy().getPolicyName(),
                DateTimes.toIsoString(history.getIssuedDate()),
                history.getValidUntil(),
                history.getDirectUtter(),
                history.getConfirmedByUser(),
                DateTimes.toIsoString(history.getCreatedAt()));
    }

    /** ISO-8601 문자열(오프셋 유무 모두 허용)을 KST 기준 LocalDateTime으로 변환한다. */
    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).atZoneSameInstant(DateTimes.KST).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(raw);
            } catch (DateTimeParseException e) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
