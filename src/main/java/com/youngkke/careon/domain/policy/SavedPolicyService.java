package com.youngkke.careon.domain.policy;

import com.youngkke.careon.domain.document.ConnectPolicyDocument;
import com.youngkke.careon.domain.document.ConnectPolicyDocumentRepository;
import com.youngkke.careon.domain.document.Document;
import com.youngkke.careon.domain.document.DocumentIssueRepository;
import com.youngkke.careon.domain.notification.NotificationRepository;
import com.youngkke.careon.domain.policy.dto.AppSavedPolicyResponse;
import com.youngkke.careon.domain.policy.dto.PolicyDetailResponse.DocumentDetail;
import com.youngkke.careon.domain.policy.dto.PolicyDetailResponse.IssuerDetail;
import com.youngkke.careon.domain.policy.dto.SavePolicyRequest;
import com.youngkke.careon.domain.policy.dto.SavePolicyResponse;
import com.youngkke.careon.domain.policy.dto.SavedPolicyResponse;
import com.youngkke.careon.domain.todo.Todo;
import com.youngkke.careon.domain.todo.TodoRepository;
import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.global.dto.MessageResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private final DocumentIssueRepository documentIssueRepository;
    private final NotificationRepository notificationRepository;
    private final TodoRepository todoRepository;

    /** 제도 저장. 저장 성공 시 connect_policy_document 기준으로 필요 서류 투두를 자동 생성한다. */
    @Transactional
    public SavePolicyResponse save(Integer userId, SavePolicyRequest request) {
        Carer carer = getCarerOrThrow(userId);
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
            // 거의 동시에 두 번 저장 요청이 들어와 위의 existsBy 체크를 둘 다 통과한 경우,
            // DB의 (user_id, policy_id) 유니크 제약이 최종 방어선 역할을 한다.
            throw new BusinessException(ErrorCode.SAVED_POLICY_ALREADY_EXISTS);
        }

        List<ConnectPolicyDocument> requiredDocuments = connectPolicyDocumentRepository.findByPolicy(policy);
        for (ConnectPolicyDocument connectPolicyDocument : requiredDocuments) {
            todoRepository.save(Todo.builder()
                    .savedPolicy(savedPolicy)
                    .document(connectPolicyDocument.getDocument())
                    .checked(false)
                    .build());
        }

        return new SavePolicyResponse(savedPolicy.getSavedPolicyId(), "제도가 저장되었습니다.");
    }

    /** 제도 저장 취소. 본인 소유가 아니면 존재하지 않는 것으로 취급하고, 연관된 투두/알림도 함께 삭제한다. */
    @Transactional
    public MessageResponse cancel(Integer userId, Integer savedPolicyId) {
        Carer carer = getCarerOrThrow(userId);
        SavedPolicy savedPolicy = savedPolicyRepository
                .findBySavedPolicyIdAndCarer(savedPolicyId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVED_POLICY_NOT_FOUND));

        notificationRepository.deleteAllBySavedPolicyIn(List.of(savedPolicy));
        todoRepository.deleteAllBySavedPolicyIn(List.of(savedPolicy));
        savedPolicyRepository.delete(savedPolicy);

        return new MessageResponse("저장이 취소되었습니다.");
    }

    /** 저장한 제도 목록 조회 (웹). */
    public List<SavedPolicyResponse> getWebList(Integer userId) {
        Carer carer = getCarerOrThrow(userId);
        return savedPolicyRepository.findAllByCarer(carer).stream()
                .map(this::toSavedPolicyResponse)
                .toList();
    }

    private SavedPolicyResponse toSavedPolicyResponse(SavedPolicy savedPolicy) {
        Policy policy = savedPolicy.getPolicy();
        List<DocumentDetail> documents = connectPolicyDocumentRepository.findByPolicy(policy).stream()
                .map(ConnectPolicyDocument::getDocument)
                .map(this::toDocumentDetail)
                .toList();

        return new SavedPolicyResponse(
                savedPolicy.getSavedPolicyId(),
                policy.getPolicyId(),
                policy.getPolicyName(),
                policy.getPolicyType().getTypeName(),
                policy.getAgency().getAgencyName(),
                policy.getSummary(),
                toDateString(policy.getApplicationDeadline()),
                toDateString(policy.getResultDate()),
                documents);
    }

    private DocumentDetail toDocumentDetail(Document document) {
        List<IssuerDetail> issuers = documentIssueRepository.findByDocument(document).stream()
                .map(issue -> new IssuerDetail(
                        issue.getDocumentIssuer().getIssuerName(), issue.getDocumentIssuer().getIssuerSite()))
                .toList();
        return new DocumentDetail(document.getDocumentId(), document.getDocumentName(), issuers);
    }

    private String toDateString(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate().toString() : null;
    }

    /**
     * 저장한 제도 목록 조회 (앱, 캘린더용). 저장한 제도 하나가 마감일/발표일을 동시에 가질 수 있어서,
     * 마감일이 있으면 마감일 카드 항목을, 발표일이 있으면 발표일 카드 항목을 각각 별도로 만든다.
     * D- 그룹(아직 지나지 않음)을 임박한 순으로 먼저, D+ 그룹(이미 지남)을 최근 지난 순으로 그 뒤에 이어붙인다.
     * D-Day까지는 디데이/필요 서류를 그대로 내려주고, D+1부터는 디데이 배지와 필요 서류를 내려주지 않는다
     * (프론트에서는 마감일 카드는 "제도 이름"만, 발표일 카드는 "제도 이름 결과 발표일"만 표시).
     */
    public List<AppSavedPolicyResponse> getAppList(Integer userId) {
        Carer carer = getCarerOrThrow(userId);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        List<AppCalendarEntry> entries = new ArrayList<>();
        for (SavedPolicy savedPolicy : savedPolicyRepository.findAllByCarer(carer)) {
            Policy policy = savedPolicy.getPolicy();

            if (policy.getApplicationDeadline() != null) {
                LocalDate deadlineDate = policy.getApplicationDeadline().toLocalDate();
                long diff = ChronoUnit.DAYS.between(today, deadlineDate);
                boolean isPast = diff < 0;
                List<String> documentNames = isPast
                        ? List.of()
                        : connectPolicyDocumentRepository.findByPolicy(policy).stream()
                                .map(cpd -> cpd.getDocument().getDocumentName())
                                .toList();
                entries.add(new AppCalendarEntry(
                        new AppSavedPolicyResponse(
                                policy.getPolicyName(),
                                deadlineDate.toString(),
                                isPast ? null : formatDDay(diff),
                                documentNames,
                                null,
                                null),
                        diff));
            }

            if (policy.getResultDate() != null) {
                LocalDate resultDate = policy.getResultDate().toLocalDate();
                long diff = ChronoUnit.DAYS.between(today, resultDate);
                boolean isPast = diff < 0;
                entries.add(new AppCalendarEntry(
                        new AppSavedPolicyResponse(
                                policy.getPolicyName(),
                                null,
                                null,
                                List.of(),
                                resultDate.toString(),
                                isPast ? null : formatDDay(diff)),
                        diff));
            }
        }

        return entries.stream()
                .sorted(Comparator.<AppCalendarEntry>comparingInt(e -> e.diff() < 0 ? 1 : 0)
                        .thenComparingLong(e -> Math.abs(e.diff())))
                .map(AppCalendarEntry::response)
                .toList();
    }

    /** diff(오늘부터 남은 일수)가 0 이상, 즉 아직 지나지 않은 날짜에 대해서만 호출된다. */
    private String formatDDay(long diff) {
        return diff == 0 ? "D-Day" : "D-" + diff;
    }

    private record AppCalendarEntry(AppSavedPolicyResponse response, long diff) {}

    private Carer getCarerOrThrow(Integer userId) {
        return carerRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
