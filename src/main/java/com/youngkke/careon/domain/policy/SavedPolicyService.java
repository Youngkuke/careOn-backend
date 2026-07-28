package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.document.ConnectPolicyDocument;
import com.youngkke.careon.domain.document.ConnectPolicyDocumentRepository;
import com.youngkke.careon.domain.notification.NotificationRepository;
import com.youngkke.careon.domain.policy.dto.AppSavedPolicyResponse;
import com.youngkke.careon.domain.policy.dto.BenefitStatusRequest;
import com.youngkke.careon.domain.policy.dto.BenefitStatusResponse;
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
import java.util.Objects;
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
    private final MatchedPolicyRepository matchedPolicyRepository;
    private final CbInstitutionReader cbInstitutionReader;

    /**
     * 제도 저장. policyId(기존 제도) 또는 servId(cb 제도) 중 하나로 저장한다.
     * 기존 제도는 connect_policy_documents 기준으로 필요 서류 투두를 자동 생성한다.
     */
    @Transactional
    public SavePolicyResponse save(Integer carerId, SavePolicyRequest request) {
        if (!request.hasExactlyOneTarget()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Carer carer = getCarerOrThrow(carerId);
        return request.policyId() != null ? savePolicy(carer, request.policyId()) : saveCbInstitution(carer, request.servId());
    }

    private SavePolicyResponse savePolicy(Carer carer, Integer policyId) {
        Policy policy = policyRepository.findById(policyId)
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
                savedPolicy.getSavedPolicyId(), policy.getPolicyId(), null, false, "제도가 저장되었습니다.");
    }

    /**
     * cb(복지로) 제도 저장. 필요 서류 데이터가 아직 없어 투두는 만들지 않는다.
     * 존재하지 않는 servId면 POLICY_NOT_FOUND로 답한다. 운영이 끝난(is_active=false) 제도는 저장을 막지 않는다.
     * 이미 찜해둔 제도가 나중에 종료될 수도 있어, 저장 시점에만 막으면 기준이 들쭉날쭉해지기 때문이다.
     */
    private SavePolicyResponse saveCbInstitution(Carer carer, String servId) {
        cbInstitutionReader.findByServId(servId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));

        if (savedPolicyRepository.existsByCarerAndServId(carer, servId)) {
            throw new BusinessException(ErrorCode.SAVED_POLICY_ALREADY_EXISTS);
        }

        SavedPolicy savedPolicy;
        try {
            savedPolicy = savedPolicyRepository.save(
                    SavedPolicy.builder().carer(carer).servId(servId).build());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.SAVED_POLICY_ALREADY_EXISTS);
        }

        return new SavePolicyResponse(
                savedPolicy.getSavedPolicyId(), null, servId, false, "제도가 저장되었습니다.");
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
     * 마감 지난 제도 또는 상시 신청 제도에 대해 "신청했어요(예)"를 기록한다. (앱 전용)
     * 신청 완료 후에도 투두 목록에는 계속 포함되며, is_applied=true로 구분된다.
     */
    @Transactional
    public SavedPolicyAppliedResponse markApplied(Integer carerId, Integer savedPolicyId) {
        Carer carer = getCarerOrThrow(carerId);
        SavedPolicy savedPolicy = savedPolicyRepository
                .findBySavedPolicyIdAndCarer(savedPolicyId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVED_POLICY_NOT_FOUND));

        savedPolicy.markApplied();

        return new SavedPolicyAppliedResponse(
                savedPolicy.getSavedPolicyId(),
                savedPolicy.isApplied(),
                savedPolicy.getApplicationStatus(),
                DateTimes.toIsoString(savedPolicy.getAppliedAt()),
                "저장되었습니다.");
    }

    /**
     * 저장한 제도 목록 조회 (웹). 카드 표시/상세 진입 정보에 더해 신청/수혜 상태를 함께 반환한다.
     * matched_policy_id는 saved_policy와 FK로 연결돼 있지 않아 (carer, policy) 기준 최선 조회 결과이며,
     * 매칭 이력이 없는 제도는 null이다.
     */
    public List<SavedPolicyResponse> getWebList(Integer carerId) {
        Carer carer = getCarerOrThrow(carerId);
        List<SavedPolicy> savedPolicies = savedPolicyRepository.findAllWithPolicyByCarer(carer);

        List<Policy> policies = savedPolicies.stream()
                .map(SavedPolicy::getPolicy)
                .filter(Objects::nonNull)
                .toList();
        Map<Integer, List<PolicyTypeSummary>> typesByPolicy = policySupport.loadPolicyTypes(policies);
        Map<String, CbInstitutionReader.CbInstitution> cbInstitutions = loadCbInstitutions(savedPolicies);

        return savedPolicies.stream()
                .map(savedPolicy -> savedPolicy.isCbInstitution()
                        ? toCbWebResponse(savedPolicy, cbInstitutions.get(savedPolicy.getServId()))
                        : toPolicyWebResponse(savedPolicy, carer, typesByPolicy))
                .toList();
    }

    private SavedPolicyResponse toPolicyWebResponse(
            SavedPolicy savedPolicy, Carer carer, Map<Integer, List<PolicyTypeSummary>> typesByPolicy) {
        Policy policy = savedPolicy.getPolicy();
        Integer matchedPolicyId = matchedPolicyRepository
                .findFirstByCarerAndPolicy(carer, policy)
                .map(MatchedPolicy::getMatchedPolicyId)
                .orElse(null);

        return new SavedPolicyResponse(
                savedPolicy.getSavedPolicyId(),
                policy.getPolicyId(),
                null,
                true,
                matchedPolicyId,
                policy.getPolicyName(),
                policy.getAgency().getAgencyId(),
                policy.getAgency().getAgencyName(),
                policy.getSummary(),
                policy.getSupportPeriod(),
                DateTimes.toIsoString(policy.getApplicationDeadline()),
                savedPolicy.isApplied(),
                savedPolicy.getApplicationStatus().name(),
                DateTimes.toIsoString(savedPolicy.getAppliedAt()),
                DateTimes.toIsoString(policy.getResultDate()),
                savedPolicy.getBenefitStatus().name(),
                DateTimes.toIsoString(savedPolicy.getBenefitCheckedAt()),
                policy.getLink(),
                typesByPolicy.getOrDefault(policy.getPolicyId(), List.of()),
                policySupport.loadDocuments(policy),
                null);
    }

    /**
     * cb 제도는 기관을 별도 테이블로 관리하지 않아 agencyId가 없고, 제도 유형·필요 서류 데이터도 아직 없다.
     * 마감일은 cb.deadline-column 설정이 있을 때만 채워지고, 없으면 기존처럼 null이다.
     * 원본 행이 사라지는 일은 없다고 확인했지만, 만에 하나 못 찾더라도 찜 항목 자체는 목록에 남긴다.
     */
    private SavedPolicyResponse toCbWebResponse(
            SavedPolicy savedPolicy, CbInstitutionReader.CbInstitution institution) {
        return new SavedPolicyResponse(
                savedPolicy.getSavedPolicyId(),
                null,
                savedPolicy.getServId(),
                institution == null || institution.active(),
                null,
                institution == null ? null : institution.name(),
                null,
                institution == null ? null : institution.agencyName(),
                institution == null ? null : institution.summary(),
                null,
                toCbDeadlineString(institution),
                savedPolicy.isApplied(),
                savedPolicy.getApplicationStatus().name(),
                DateTimes.toIsoString(savedPolicy.getAppliedAt()),
                toIsoDate(institution == null ? null : institution.resultDate()),
                savedPolicy.getBenefitStatus().name(),
                DateTimes.toIsoString(savedPolicy.getBenefitCheckedAt()),
                institution == null ? null : institution.link(),
                List.of(),
                List.of(),
                institution == null ? null : institution.regionName());
    }

    private String toCbDeadlineString(CbInstitutionReader.CbInstitution institution) {
        return toIsoDate(institution == null ? null : institution.deadline());
    }

    /**
     * cb의 마감일·발표일은 날짜만 있어 시각이 없다. 기존 제도와 응답 형식이 갈리면 앱이 두 가지로 파싱해야
     * 하므로 자정 기준 ISO 문자열로 맞춰서 내려보낸다.
     */
    private String toIsoDate(java.time.LocalDate date) {
        return date == null ? null : DateTimes.toIsoString(date.atStartOfDay());
    }

    private Map<String, CbInstitutionReader.CbInstitution> loadCbInstitutions(List<SavedPolicy> savedPolicies) {
        return cbInstitutionReader.findAllByServIds(savedPolicies.stream()
                .map(SavedPolicy::getServId)
                .filter(Objects::nonNull)
                .toList());
    }

    /**
     * 웹에서 신청 완료한 제도의 수혜 여부를 기록/수정한다.
     * 결과 발표일 유무와 관계없이 유저가 언제든 직접 입력할 수 있다.
     */
    @Transactional
    public BenefitStatusResponse updateBenefitStatus(
            Integer carerId, Integer savedPolicyId, BenefitStatusRequest request) {
        Carer carer = getCarerOrThrow(carerId);
        SavedPolicy savedPolicy = savedPolicyRepository
                .findBySavedPolicyIdAndCarer(savedPolicyId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVED_POLICY_NOT_FOUND));

        savedPolicy.updateBenefitStatus(request.benefitStatus());

        return new BenefitStatusResponse(
                savedPolicy.getSavedPolicyId(),
                savedPolicy.getBenefitStatus().name(),
                DateTimes.toIsoString(savedPolicy.getBenefitCheckedAt()),
                "수혜 여부가 수정되었습니다.");
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

        List<SavedPolicy> savedPolicies = savedPolicyRepository.findAllWithPolicyByCarer(carer);
        Map<String, CbInstitutionReader.CbInstitution> cbInstitutions = loadCbInstitutions(savedPolicies);

        for (SavedPolicy savedPolicy : savedPolicies) {
            if (savedPolicy.isCbInstitution()) {
                // cb 제도는 마감일·발표일 데이터가 없어 D-day를 계산할 수 없다. 캘린더에서는 날짜 없는 항목으로 뒤에 놓인다.
                CbInstitutionReader.CbInstitution institution = cbInstitutions.get(savedPolicy.getServId());
                entries.add(new Entry(
                        new AppSavedPolicyResponse(
                                savedPolicy.getSavedPolicyId(),
                                null,
                                savedPolicy.getServId(),
                                institution == null ? null : institution.name(),
                                null,
                                null,
                                List.of(),
                                null,
                                null),
                        Long.MAX_VALUE));
                continue;
            }

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
                    null,
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
