package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.document.ConnectPolicyDocument;
import com.youngkke.careon.domain.document.ConnectPolicyDocumentRepository;
import com.youngkke.careon.domain.notification.NotificationRepository;
import com.youngkke.careon.domain.policy.dto.AppSavedPolicyResponse;
import com.youngkke.careon.domain.policy.dto.SavePolicyRequest;
import com.youngkke.careon.domain.policy.dto.SavePolicyResponse;
import com.youngkke.careon.domain.policy.dto.SavedPolicyAppliedResponse;
import com.youngkke.careon.domain.policy.dto.SavedPolicyResponse;
import com.youngkke.careon.domain.policy.dto.PolicyTypeSummary;
import com.youngkke.careon.domain.todo.Todo;
import com.youngkke.careon.domain.todo.TodoRepository;
import com.youngkke.careon.global.dto.MessageResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedPolicyService {

    private final SavedPolicyRepository savedPolicyRepository;
    private final PolicyRepository policyRepository;
    private final CarerRepository carerRepository;
    private final ConnectPolicyDocumentRepository connectPolicyDocumentRepository;
    private final NotificationRepository notificationRepository;
    private final TodoRepository todoRepository;
    private final PolicySupport policySupport;

    /** 제도 저장. 저장 성공 시 connect_policy_documents 기준으로 필요 서류 투두를 자동 생성한다. */
    @Transactional
    public SavePolicyResponse save(Integer carerId, SavePolicyRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        Policy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        if (savedPolicyRepository.existsByCarerAndPolicy(carer, policy)) {
            throw new BusinessException(ErrorCode.SAVED_POLICY_ALREADY_EXISTS);
        }

        SavedPolicy savedPolicy;
        try {
            savedPolicy = savedPolicyRepository.save(
                    SavedPolicy.builder().carer(carer).policy(policy).build());
        } catch (DataIntegrityViolationException e) {
            // 거의 동시에 두 번 저장 요청이 들어온 경우, DB의 (carer_id, policy_id) 유니크 제약이 최종 방어선이 된다.
            throw new BusinessException(ErrorCode.SAVED_POLICY_ALREADY_EXISTS);
        }

        for (ConnectPolicyDocument connect : connectPolicyDocumentRepository.findByPolicy(policy)) {
            todoRepository.save(Todo.builder()
                    .savedPolicy(savedPolicy)
                    .document(connect.getDocument())
                    .checked(false)
                    .build());
        }

        return new SavePolicyResponse(
                savedPolicy.getSavedPolicyId(), policy.getPolicyId(), false, "제도가 저장되었습니다.");
    }

    /** 제도 저장 취소. 본인 소유가 아니면 존재하지 않는 것으로 취급하고, 연관된 투두/알림도 함께 삭제한다. */
    @Transactional
    public MessageResponse cancel(Integer carerId, Integer savedPolicyId) {
        Carer carer = getCarerOrThrow(carerId);
        SavedPolicy savedPolicy = savedPolicyRepository
                .findBySavedPolicyIdAndCarer(savedPolicyId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVED_POLICY_NOT_FOUND));

        notificationRepository.deleteAllBySavedPolicyIn(List.of(savedPolicy));
        todoRepository.deleteAllBySavedPolicyIn(List.of(savedPolicy));
        savedPolicyRepository.delete(savedPolicy);

        return new MessageResponse("저장이 취소되었습니다.");
    }

    /**
     * 마감 지난 저장 제도에 대해 "신청했어요(예)"를 기록한다. (앱 전용)
     * 기록된 제도는 이후 투두 목록 조회에서 제외된다.
     */
    @Transactional
    public SavedPolicyAppliedResponse markApplied(Integer carerId, Integer savedPolicyId) {
        Carer carer = getCarerOrThrow(carerId);
        SavedPolicy savedPolicy = savedPolicyRepository
                .findBySavedPolicyIdAndCarer(savedPolicyId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVED_POLICY_NOT_FOUND));

        savedPolicy.markApplied();

        return new SavedPolicyAppliedResponse(
                savedPolicy.getSavedPolicyId(), savedPolicy.getApplied(), "저장되었습니다.");
    }

    /** 저장한 제도 목록 조회 (웹). 카드 표시/상세 진입용 제도 정보만 반환한다. */
    public List<SavedPolicyResponse> getWebList(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        List<SavedPolicy> savedPolicies = savedPolicyRepository.findAllWithPolicyByCarer(carer);

        List<Policy> policies = savedPolicies.stream().map(SavedPolicy::getPolicy).toList();
        Map<Integer, List<PolicyTypeSummary>> typesByPolicy = policySupport.loadPolicyTypes(policies);

        return savedPolicies.stream()
                .map(savedPolicy -> {
                    Policy policy = savedPolicy.getPolicy();
                    return new SavedPolicyResponse(
                            savedPolicy.getSavedPolicyId(),
                            policy.getPolicyId(),
                            policy.getPolicyName(),
                            policy.getAgency().getAgencyId(),
                            policy.getAgency().getAgencyName(),
                            policy.getSummary(),
                            policy.getSupportPeriod(),
                            DateTimes.toIsoString(policy.getApplicationDeadline()),
                            policy.getLink(),
                            typesByPolicy.getOrDefault(policy.getPolicyId(), List.of()),
                            policySupport.loadDocuments(policy));
                })
                .toList();
    }

    /**
     * 저장한 제도 목록 조회 (앱 캘린더).
     * 지난 날짜도 계속 표시하지만 D+ 표기는 하지 않는다.
     * 정렬은 예정 항목을 임박한 순으로 먼저, 지난 항목을 최근 지난 순으로 뒤에 이어붙인다.
     * (한 항목이 마감일/발표일을 모두 가질 수 있어, 정렬 기준일은 마감일이 있으면 마감일, 없으면 발표일을 쓴다)
     */
    public List<AppSavedPolicyResponse> getAppList(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        LocalDate today = DateTimes.today();

        record Entry(AppSavedPolicyResponse response, long diff) {}
        List<Entry> entries = new ArrayList<>();

        for (SavedPolicy savedPolicy : savedPolicyRepository.findAllWithPolicyByCarer(carer)) {
            Policy policy = savedPolicy.getPolicy();
            LocalDate deadline = toLocalDate(policy.getApplicationDeadline());
            LocalDate resultDate = toLocalDate(policy.getResultDate());

            boolean deadlineExpired = deadline != null && deadline.isBefore(today);
            List<String> documentNames = (deadline == null || deadlineExpired)
                    ? List.of()
                    : connectPolicyDocumentRepository.findByPolicy(policy).stream()
                            .map(connect -> connect.getDocument().getDocumentName())
                            .toList();

            AppSavedPolicyResponse response = new AppSavedPolicyResponse(
                    savedPolicy.getSavedPolicyId(),
                    policy.getPolicyId(),
                    policy.getPolicyName(),
                    deadline == null ? null : deadline.toString(),
                    toDDay(deadline, today),
                    documentNames,
                    resultDate == null ? null : resultDate.toString(),
                    toDDay(resultDate, today));

            LocalDate baseDate = deadline != null ? deadline : resultDate;
            long diff = baseDate == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(today, baseDate);
            entries.add(new Entry(response, diff));
        }

        return entries.stream()
                .sorted(Comparator.<Entry>comparingInt(entry -> entry.diff() < 0 ? 1 : 0)
                        .thenComparingLong(entry -> Math.abs(entry.diff())))
                .map(Entry::response)
                .toList();
    }

    /** 예정 날짜만 D-표기하고, 이미 지난 날짜는 null을 반환한다. */
    private String toDDay(LocalDate date, LocalDate today) {
        if (date == null) {
            return null;
        }
        long diff = ChronoUnit.DAYS.between(today, date);
        if (diff < 0) {
            return null;
        }
        return diff == 0 ? "D-Day" : "D-" + diff;
    }

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private Carer getCarerOrThrow(Integer carerId) {
        return carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
