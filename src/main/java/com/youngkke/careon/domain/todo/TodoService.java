package com.youngkke.careon.domain.todo;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.document.DocumentIssueRepository;
import com.youngkke.careon.domain.document.dto.IssuerSummary;
import com.youngkke.careon.domain.policy.ApplicationPeriodType;
import com.youngkke.careon.domain.policy.CbInstitutionReader;
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
import java.util.Map;
import java.util.Objects;
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
    private final CbInstitutionReader cbInstitutionReader;

    /**
     * 투두 목록 조회 (앱).
     * - 신청 완료 제도(application_status=APPLIED)도 포함해서 반환한다. 프런트가 is_applied로 탭을 나눈다.
     * - 마감일이 있는 제도: 마감 전에는 is_expired=false, 마감 지나면 is_expired=true (신청 여부 질문용).
     * - 마감일이 없는(상시/누락) 제도: is_expired는 항상 false. application_period_type으로 상시 여부를 구분한다.
     * - 서류 체크리스트는 아직 신청 전(PREPARING)이고 마감 전(is_expired=false)일 때만 내려준다.
     * - cb 제도도 기존 제도와 같은 규칙으로 포함한다. 제도명·마감일·발표일·링크는 cb에서 읽고,
     *   서류와 체크 상태는 저장 시점에 만들어 둔 우리 todos 행에서 읽는다.
     */
    public List<TodoListResponse> getList(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        LocalDate today = DateTimes.today();

        List<SavedPolicy> savedPolicies = savedPolicyRepository.findAllWithPolicyByCarer(carer);
        Map<String, CbInstitutionReader.CbInstitution> cbInstitutions =
                cbInstitutionReader.findAllByServIds(savedPolicies.stream()
                        .map(SavedPolicy::getServId)
                        .filter(Objects::nonNull)
                        .toList());

        return savedPolicies.stream()
                .map(savedPolicy -> savedPolicy.isCbInstitution()
                        ? toCbTodoListResponse(savedPolicy, cbInstitutions.get(savedPolicy.getServId()), today)
                        : toTodoListResponse(savedPolicy, today))
                .toList();
    }

    private TodoListResponse toTodoListResponse(SavedPolicy savedPolicy, LocalDate today) {
        Policy policy = savedPolicy.getPolicy();
        LocalDateTime deadline = policy.getApplicationDeadline();
        boolean expired = deadline != null && deadline.toLocalDate().isBefore(today);

        return new TodoListResponse(
                savedPolicy.getSavedPolicyId(),
                policy.getPolicyId(),
                null,
                policy.getPolicyName(),
                deadline == null ? null : deadline.toLocalDate().toString(),
                policy.getApplicationPeriodTypeOrDefault().name(),
                expired,
                savedPolicy.getApplicationStatus().name(),
                savedPolicy.isApplied(),
                DateTimes.toIsoString(savedPolicy.getAppliedAt()),
                DateTimes.toDateString(policy.getResultDate()),
                policy.getLink(),
                loadDocuments(savedPolicy, expired));
    }

    /**
     * cb 제도의 투두 항목.
     *
     * <p>cb에는 상시 신청 여부를 알려주는 칸이 없어, 기존 제도가 값을 채우지 못했을 때와 같은 기준으로
     * 추정한다. 마감일이 있으면 FIXED, 없으면 UNKNOWN이다. (상시 신청 확정은 데이터가 명시해야 한다)
     *
     * <p>원본 행을 못 찾더라도 항목 자체는 남긴다. 사용자가 찜한 기록이 화면에서 조용히 사라지는 것보다
     * 이름 없는 항목으로라도 보이는 편이 낫다.
     */
    private TodoListResponse toCbTodoListResponse(
            SavedPolicy savedPolicy, CbInstitutionReader.CbInstitution institution, LocalDate today) {
        LocalDate deadline = institution == null ? null : institution.deadline();
        LocalDate resultDate = institution == null ? null : institution.resultDate();
        boolean expired = deadline != null && deadline.isBefore(today);
        ApplicationPeriodType periodType =
                deadline != null ? ApplicationPeriodType.FIXED : ApplicationPeriodType.UNKNOWN;

        return new TodoListResponse(
                savedPolicy.getSavedPolicyId(),
                null,
                savedPolicy.getServId(),
                institution == null ? null : institution.name(),
                deadline == null ? null : deadline.toString(),
                periodType.name(),
                expired,
                savedPolicy.getApplicationStatus().name(),
                savedPolicy.isApplied(),
                DateTimes.toIsoString(savedPolicy.getAppliedAt()),
                resultDate == null ? null : resultDate.toString(),
                institution == null ? null : institution.link(),
                loadDocuments(savedPolicy, expired));
    }

    /** 서류 체크리스트는 아직 신청 전이고 마감 전일 때만 내려준다. 두 갈래 제도에 같은 규칙을 쓴다. */
    private List<TodoDocumentDetail> loadDocuments(SavedPolicy savedPolicy, boolean expired) {
        if (expired || savedPolicy.isApplied()) {
            return List.of();
        }
        return todoRepository.findAllBySavedPolicy(savedPolicy).stream()
                .map(this::toTodoDocumentDetail)
                .toList();
    }

    /**
     * cb 서류는 우리 documents 테이블에 행이 없어 documentId와 발급처가 없다. 대신 발급처/서식 링크가
     * 하나 붙을 수 있어 documentUrl로 내려간다. 기존 제도는 반대로 issuers가 채워지고 url이 없다.
     */
    private TodoDocumentDetail toTodoDocumentDetail(Todo todo) {
        if (todo.isCbDocument()) {
            return new TodoDocumentDetail(
                    todo.getTodoId(),
                    null,
                    todo.getDocumentName(),
                    List.of(),
                    todo.getDocumentUrl(),
                    todo.getDocumentUrlType(),
                    todo.isChecked());
        }

        List<IssuerSummary> issuers = documentIssueRepository.findByDocument(todo.getDocument()).stream()
                .map(issue -> IssuerSummary.from(issue.getDocumentIssuer()))
                .toList();
        return new TodoDocumentDetail(
                todo.getTodoId(),
                todo.getDocument().getDocumentId(),
                todo.getDocument().getDocumentName(),
                issuers,
                null,
                null,
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
