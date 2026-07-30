package com.youngkke.careon.domain.todo;

import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.document.CbDocumentIssuer;
import com.youngkke.careon.domain.document.CbDocumentIssuerRepository;
import com.youngkke.careon.domain.document.DocumentIssue;
import com.youngkke.careon.domain.document.DocumentIssueRepository;
import com.youngkke.careon.domain.document.DocumentIssuer;
import com.youngkke.careon.domain.document.DocumentIssuerRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final DocumentIssuerRepository documentIssuerRepository;
    private final CbDocumentIssuerRepository cbDocumentIssuerRepository;
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
        if (savedPolicies.isEmpty()) {
            return List.of();
        }

        Map<String, CbInstitutionReader.CbInstitution> cbInstitutions =
                cbInstitutionReader.findAllByServIds(savedPolicies.stream()
                        .map(SavedPolicy::getServId)
                        .filter(Objects::nonNull)
                        .toList());

        Map<Integer, List<Todo>> todosBySavedPolicy = todoRepository
                .findAllWithDocumentBySavedPolicyIn(savedPolicies).stream()
                .collect(Collectors.groupingBy(todo -> todo.getSavedPolicy().getSavedPolicyId()));
        CbIssuerLookup cbIssuerLookup = createCbIssuerLookup(todosBySavedPolicy);

        return savedPolicies.stream()
                .map(savedPolicy -> {
                    List<Todo> todos = todosBySavedPolicy.getOrDefault(savedPolicy.getSavedPolicyId(), List.of());
                    return savedPolicy.isCbInstitution()
                            ? toCbTodoListResponse(
                                    savedPolicy, cbInstitutions.get(savedPolicy.getServId()), todos, cbIssuerLookup, today)
                            : toTodoListResponse(savedPolicy, todos, cbIssuerLookup, today);
                })
                .toList();
    }

    /**
     * cb 서류의 발급처를 찾기 위한 조회표.
     *
     * <p>cb 서류는 우리 documents 테이블의 행이 아니라 이름만 있어서 발급처가 딸려오지 않는다.
     * 그래서 두 가지를 미리 모아둔다. 이름으로 발급처를 찾고, 못 찾으면 cb가 준 링크의 도메인으로 찾는다.
     *
     * @param issuersByDocumentName 서류 이름 -> 발급처 목록. 우리 마스터에 같은 이름의 서류가 있으면 그 발급처를
     *     빌려 쓰고, 없으면 cb 전용 매핑(cb_document_issuers)을 본다
     * @param issuerByHost 도메인(www 제외) -> 발급처
     * @param fallback 위 어느 쪽으로도 못 찾았을 때 쓸 발급처("신청 공고 확인"). 마스터에 그 행이 없으면 null이다
     */
    private record CbIssuerLookup(
            Map<String, List<IssuerSummary>> issuersByDocumentName,
            Map<String, DocumentIssuer> issuerByHost,
            IssuerSummary fallback) {}

    /**
     * 어느 쪽으로도 발급처를 못 찾았을 때 쓰는 값.
     *
     * <p>cb 서류 이름은 AI가 제도마다 새로 생성해서 계속 늘어난다. 실제로 "개인정보제공 동의서"가 이미
     * 매핑돼 있는데도 띄어쓰기가 하나 다른 "개인정보 제공 동의서"가 새로 들어와 발급처가 비었다.
     * 이름을 하나씩 채우는 방식으로는 따라잡을 수 없다.
     *
     * <p>"신청 공고 확인"은 어떤 서류에 붙여도 거짓이 되지 않는다. 그 서류를 요구한 제도의 공고에는
     * 발급처가 적혀 있기 때문이다. 그래서 모르는 서류에 지어낸 기관을 붙이는 것과 달리 안전하다.
     */
    private static final String FALLBACK_ISSUER_NAME = "신청 공고 확인";

    private CbIssuerLookup createCbIssuerLookup(Map<Integer, List<Todo>> todosBySavedPolicy) {
        List<String> cbDocumentNames = todosBySavedPolicy.values().stream()
                .flatMap(List::stream)
                .filter(Todo::isCbDocument)
                .map(Todo::getDocumentName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (cbDocumentNames.isEmpty()) {
            return new CbIssuerLookup(Map.of(), Map.of(), null);
        }

        Map<String, List<IssuerSummary>> issuersByName = new HashMap<>();
        for (DocumentIssue issue : documentIssueRepository.findAllWithIssuerByDocumentNameIn(cbDocumentNames)) {
            List<IssuerSummary> issuers =
                    issuersByName.computeIfAbsent(issue.getDocument().getDocumentName(), key -> new ArrayList<>());
            IssuerSummary summary = IssuerSummary.from(issue.getDocumentIssuer());
            // 같은 이름의 서류가 마스터에 여러 행 있으면 발급처가 겹칠 수 있다.
            if (issuers.stream()
                    .noneMatch(existing -> Objects.equals(existing.documentIssuerId(), summary.documentIssuerId()))) {
                issuers.add(summary);
            }
        }

        // 마스터에서 못 찾은 이름만 cb 전용 매핑으로 채운다. 마스터 쪽이 서류-발급처를 더 정확히 들고 있어서다.
        // 아래 루프가 돌면서 키가 늘어나므로, 마스터로 채워진 이름을 먼저 떠 놓고 그것과 비교한다.
        Set<String> namesFromMaster = Set.copyOf(issuersByName.keySet());
        for (CbDocumentIssuer mapping : cbDocumentIssuerRepository.findAllWithIssuerByDocumentNameIn(cbDocumentNames)) {
            if (namesFromMaster.contains(mapping.getDocumentName())) {
                continue;
            }
            issuersByName
                    .computeIfAbsent(mapping.getDocumentName(), key -> new ArrayList<>())
                    .add(IssuerSummary.from(mapping.getDocumentIssuer()));
        }

        Map<String, DocumentIssuer> issuerByHost = new HashMap<>();
        IssuerSummary fallback = null;
        for (DocumentIssuer issuer : documentIssuerRepository.findAll()) {
            String host = toHost(issuer.getIssuerSite());
            if (host != null) {
                issuerByHost.putIfAbsent(host, issuer);
            }
            if (FALLBACK_ISSUER_NAME.equals(issuer.getIssuerName())) {
                fallback = IssuerSummary.from(issuer);
            }
        }

        return new CbIssuerLookup(issuersByName, issuerByHost, fallback);
    }

    /** "https://www.bokjiro.go.kr/a/b?c=d" -> "bokjiro.go.kr". 주소를 못 알아보면 null. */
    private String toHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String host = url.trim().replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "");
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        host = host.replaceFirst("^www\\.", "");
        return host.isBlank() ? null : host;
    }

    private TodoListResponse toTodoListResponse(
            SavedPolicy savedPolicy, List<Todo> todos, CbIssuerLookup cbIssuerLookup, LocalDate today) {
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
                toDocuments(savedPolicy, todos, cbIssuerLookup, expired));
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
            SavedPolicy savedPolicy,
            CbInstitutionReader.CbInstitution institution,
            List<Todo> todos,
            CbIssuerLookup cbIssuerLookup,
            LocalDate today) {
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
                toDocuments(savedPolicy, todos, cbIssuerLookup, expired));
    }

    /** 서류 체크리스트는 아직 신청 전이고 마감 전일 때만 내려준다. 두 갈래 제도에 같은 규칙을 쓴다. */
    private List<TodoDocumentDetail> toDocuments(
            SavedPolicy savedPolicy, List<Todo> todos, CbIssuerLookup cbIssuerLookup, boolean expired) {
        if (expired || savedPolicy.isApplied()) {
            return List.of();
        }
        return todos.stream()
                .map(todo -> toTodoDocumentDetail(todo, cbIssuerLookup))
                .toList();
    }

    /**
     * cb 서류는 우리 documents 테이블에 행이 없어 documentId가 없고 발급처도 딸려오지 않는다.
     * 앱은 발급처가 비어 있으면 "발급시 확인 필요"로 표시하므로, 알 수 있는 건 최대한 채워서 내려준다.
     * 어느 쪽으로도 못 찾으면 빈 목록이 맞다. 모르는 걸 아는 것처럼 지어내지는 않는다.
     */
    private TodoDocumentDetail toTodoDocumentDetail(Todo todo, CbIssuerLookup cbIssuerLookup) {
        if (todo.isCbDocument()) {
            return new TodoDocumentDetail(
                    todo.getTodoId(),
                    null,
                    todo.getDocumentName(),
                    resolveCbIssuers(todo, cbIssuerLookup),
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

    /**
     * cb 서류의 발급처를 정한다.
     *
     * <p>1순위는 우리 마스터에 같은 이름으로 등록된 서류의 발급처다. 발급처 이름과 대표 사이트가 함께
     * 관리되고 있어 화면에 그대로 쓸 수 있다.
     *
     * <p>2순위는 cb가 준 링크다. 도메인이 우리 발급처 목록과 맞으면 그 이름을 쓰고, 모르는 도메인이면
     * 도메인 자체를 이름으로 보여준다. 이때 사이트 주소는 발급처 대표 주소가 아니라 cb가 준 주소를
     * 그대로 쓴다. 서식 내려받기처럼 특정 페이지를 가리키는 링크라 대표 주소로 바꾸면 못 쓰게 된다.
     */
    private List<IssuerSummary> resolveCbIssuers(Todo todo, CbIssuerLookup cbIssuerLookup) {
        List<IssuerSummary> byName = cbIssuerLookup.issuersByDocumentName().get(todo.getDocumentName());
        if (byName != null && !byName.isEmpty()) {
            return byName;
        }

        String url = todo.getDocumentUrl();
        String host = toHost(url);
        if (host == null) {
            // 이름도 링크도 못 찾았다. 발급처를 비워 두면 앱이 "발급처 확인 필요"로 표시하는데,
            // 그건 사용자에게 다음에 뭘 하라는 말이 없다. 공고를 보라는 안내가 항상 참이라 그쪽이 낫다.
            return cbIssuerLookup.fallback() == null ? List.of() : List.of(cbIssuerLookup.fallback());
        }

        DocumentIssuer issuer = cbIssuerLookup.issuerByHost().get(host);
        // 모르는 도메인이면 안내 문구를 지어낼 수 없어 null로 둔다. 앱은 그때 발급처 이름(도메인)을 쓴다.
        return List.of(issuer == null
                ? new IssuerSummary(null, host, url, null)
                : new IssuerSummary(issuer.getDocumentIssuerId(), issuer.getIssuerName(), url, issuer.getIssueGuide()));
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
