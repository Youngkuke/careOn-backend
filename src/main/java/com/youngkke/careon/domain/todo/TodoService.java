package com.youngkke.careon.domain.todo;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.document.DocumentIssueRepository;
import com.youngkke.careon.domain.document.dto.IssuerSummary;
import com.youngkke.careon.domain.policy.Policy;
import com.youngkke.careon.domain.policy.SavedPolicy;
import com.youngkke.careon.domain.policy.SavedPolicyRepository;
import com.youngkke.careon.domain.todo.dto.TodoCheckRequest;
import com.youngkke.careon.domain.todo.dto.TodoCheckResponse;
import com.youngkke.careon.domain.todo.dto.TodoListResponse;
import com.youngkke.careon.domain.todo.dto.TodoListResponse.TodoDocumentDetail;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
     * 투두 목록 조회 (앱).
     * - 신청 완료 제도(application_status=APPLIED)도 포함해서 반환한다. 프런트가 is_applied로 탭을 나눈다.
     * - 마감일이 있는 제도: 마감 전에는 is_expired=false, 마감 지나면 is_expired=true (신청 여부 질문용).
     * - 마감일이 없는(상시/누락) 제도: is_expired는 항상 false. application_period_type으로 상시 여부를 구분한다.
     * - 서류 체크리스트는 아직 신청 전(PREPARING)이고 마감 전(is_expired=false)일 때만 내려준다.
     */
    public List<TodoListResponse> getList(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        LocalDate today = DateTimes.today();

        // cb 제도는 필요 서류 데이터가 아직 없어 준비할 서류가 하나도 없다. 빈 항목만 늘어나므로 여기서는 제외한다.
        // (찜 목록에는 그대로 보인다)
        return savedPolicyRepository.findAllWithPolicyByCarer(carer).stream()
                .filter(savedPolicy -> !savedPolicy.isCbInstitution())
                .map(savedPolicy -> toTodoListResponse(savedPolicy, today))
                .toList();
    }

    private TodoListResponse toTodoListResponse(SavedPolicy savedPolicy, LocalDate today) {
        Policy policy = savedPolicy.getPolicy();
        LocalDateTime deadline = policy.getApplicationDeadline();
        boolean expired = deadline != null && deadline.toLocalDate().isBefore(today);
        boolean applied = savedPolicy.isApplied();

        List<TodoDocumentDetail> documents = (expired || applied)
                ? List.of()
                : todoRepository.findAllBySavedPolicy(savedPolicy).stream()
                        .map(this::toTodoDocumentDetail)
                        .toList();

        return new TodoListResponse(
                savedPolicy.getSavedPolicyId(),
                policy.getPolicyId(),
                policy.getPolicyName(),
                deadline == null ? null : deadline.toLocalDate().toString(),
                policy.getApplicationPeriodTypeOrDefault().name(),
                expired,
                savedPolicy.getApplicationStatus().name(),
                applied,
                DateTimes.toIsoString(savedPolicy.getAppliedAt()),
                DateTimes.toDateString(policy.getResultDate()),
                policy.getLink(),
                documents);
    }

    private TodoDocumentDetail toTodoDocumentDetail(Todo todo) {
        List<IssuerSummary> issuers = documentIssueRepository.findByDocument(todo.getDocument()).stream()
                .map(issue -> IssuerSummary.from(issue.getDocumentIssuer()))
                .toList();
        return new TodoDocumentDetail(
                todo.getTodoId(),
                todo.getDocument().getDocumentId(),
                todo.getDocument().getDocumentName(),
                issuers,
                todo.isChecked());
    }

    /** 투두 체크/체크 해제. 본인 소유가 아니면 존재하지 않는 것으로 취급한다. */
    @Transactional
    public TodoCheckResponse updateChecked(Integer carerId, Integer todoId, TodoCheckRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        Todo todo = todoRepository
                .findByTodoIdAndSavedPolicy_Carer(todoId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));

        todo.updateChecked(request.isChecked());

        return new TodoCheckResponse(todo.getTodoId(), todo.isChecked(), "체크 상태가 변경되었습니다.");
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
