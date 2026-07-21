package com.youngkke.careon.domain.todo;

import com.youngkke.careon.domain.document.DocumentIssueRepository;
import com.youngkke.careon.domain.policy.Policy;
import com.youngkke.careon.domain.policy.SavedPolicy;
import com.youngkke.careon.domain.policy.SavedPolicyRepository;
import com.youngkke.careon.domain.todo.dto.TodoCheckRequest;
import com.youngkke.careon.domain.todo.dto.TodoListResponse;
import com.youngkke.careon.domain.todo.dto.TodoListResponse.IssuerDetail;
import com.youngkke.careon.domain.todo.dto.TodoListResponse.TodoDocumentDetail;
import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.global.dto.MessageResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final SavedPolicyRepository savedPolicyRepository;
    private final DocumentIssueRepository documentIssueRepository;
    private final CarerRepository carerRepository;

    /**
     * 투두 목록 조회.
     * - 마감 전 제도: 서류 체크리스트 포함 (isExpired=false)
     * - 마감 지난 제도 중 신청 여부 미응답(applied가 null)인 건: 서류 없이 포함 (isExpired=true)
     *   → 프론트에서 "신청하셨나요?" 예/아니오 버튼 표시용
     * - 마감 지난 제도 중 이미 응답한 건(applied=true)은 제외
     */
    public List<TodoListResponse> getList(Integer userId) {
        Carer carer = getCarerOrThrow(userId);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        return savedPolicyRepository.findAllByCarer(carer).stream()
                .filter(savedPolicy -> {
                    Policy policy = savedPolicy.getPolicy();
                    if (policy.getApplicationDeadline() == null) {
                        return false;
                    }
                    boolean expired = policy.getApplicationDeadline().toLocalDate().isBefore(today);
                    return !expired || savedPolicy.getApplied() == null;
                })
                .map(savedPolicy -> toTodoListResponse(savedPolicy, today))
                .toList();
    }

    private TodoListResponse toTodoListResponse(SavedPolicy savedPolicy, LocalDate today) {
        Policy policy = savedPolicy.getPolicy();
        boolean expired = policy.getApplicationDeadline().toLocalDate().isBefore(today);

        List<TodoDocumentDetail> documents = expired
                ? List.of()
                : todoRepository.findAllBySavedPolicy(savedPolicy).stream()
                        .map(this::toTodoDocumentDetail)
                        .toList();

        return new TodoListResponse(
                savedPolicy.getSavedPolicyId(),
                policy.getPolicyId(),
                policy.getPolicyName(),
                policy.getApplicationDeadline().toLocalDate().toString(),
                policy.getLink(),
                expired,
                documents);
    }

    private TodoDocumentDetail toTodoDocumentDetail(Todo todo) {
        List<IssuerDetail> issuers = documentIssueRepository.findByDocument(todo.getDocument()).stream()
                .map(issue -> new IssuerDetail(
                        issue.getDocumentIssuer().getIssuerName(), issue.getDocumentIssuer().getIssuerSite()))
                .toList();
        return new TodoDocumentDetail(
                todo.getTodoId(), todo.getDocument().getDocumentId(), todo.getDocument().getDocumentName(), issuers,
                todo.isChecked());
    }

    /** 투두 체크/체크 해제. 본인 소유가 아니면 존재하지 않는 것으로 취급한다. */
    @Transactional
    public MessageResponse updateChecked(Integer userId, Integer todoId, TodoCheckRequest request) {
        Carer carer = getCarerOrThrow(userId);
        Todo todo = todoRepository
                .findByTodoIdAndSavedPolicy_Carer(todoId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));

        todo.updateChecked(request.isChecked());

        return new MessageResponse("체크 상태가 변경되었습니다.");
    }

    private Carer getCarerOrThrow(Integer userId) {
        return carerRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
