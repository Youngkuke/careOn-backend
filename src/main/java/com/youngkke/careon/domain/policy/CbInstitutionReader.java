package com.youngkke.careon.domain.policy;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 서버가 복지로 API로 채우는 cb 스키마의 제도를 읽는다. 같은 DB 안에 있어 조회로 바로 가져온다.
 *
 * <p>JPA 엔티티로 매핑하지 않고 네이티브 조회로 읽는 이유는, 이 프로젝트가 ddl-auto=update로 뜨기 때문이다.
 * 엔티티로 잡으면 Hibernate가 남의 팀 소유 테이블까지 스키마 관리 대상으로 보고 손대려 할 수 있다.
 * 우리는 읽기만 하면 되므로 필요한 칸만 뽑는다.
 *
 * <p>제도가 원본에서 사라져도 AI 서버는 행을 지우지 않고 is_active=false로 내린다. 그래서 찜해둔 제도가
 * 갑자기 조회되지 않는 일은 없고, 종료 여부만 active로 구분한다.
 */
@Component
public class CbInstitutionReader {

    private static final Logger log = LoggerFactory.getLogger(CbInstitutionReader.class);

    private static final String BASE_COLUMNS =
            "serv_id, serv_nm, serv_dgst, jur_org_nm, detail_link, ctpv_nm, sgg_nm, is_active";

    /** BASE_COLUMNS의 개수. 선택 컬럼이 이 다음 자리부터 붙는다. */
    private static final int BASE_COLUMN_COUNT = 8;

    /**
     * 필요 서류(required_documents_ai, jsonb)를 제도별로 읽는다.
     *
     * <p>JSON을 통째로 받아 자바에서 파싱하지 않고 Postgres가 풀어서 평평한 행으로 주도록 한 이유는,
     * 그게 파싱 코드도 라이브러리 의존성도 없이 끝나기 때문이다. jsonb는 이미 파싱된 상태로 저장돼 있어
     * DB 입장에서도 추가 비용이 아니다.
     *
     * <p>이름이 비어 있는 항목은 화면에 뭘 준비하라는 건지 표시할 수 없으므로 걸러낸다.
     *
     * <p>배열이 아닌 값이 들어오면 jsonb_array_elements가 에러를 낸다. 이 함수는 WHERE보다 먼저
     * 평가되므로 조건절로는 막을 수 없어, 행마다 CASE로 먼저 걸러 빈 배열로 바꾼다. AI 서버가 채우는
     * 칸이라 형식이 바뀔 수 있는데, 그때 투두 조회 전체가 실패하는 일은 없어야 한다.
     */
    private static final String SELECT_DOCUMENTS =
            """
            SELECT i.serv_id,
                   doc ->> 'name'     AS name,
                   doc ->> 'url'      AS url,
                   doc ->> 'url_type' AS url_type
            FROM cb.cb_institutions i
                     CROSS JOIN LATERAL jsonb_array_elements(
                         CASE WHEN jsonb_typeof(i.required_documents_ai) = 'array'
                              THEN i.required_documents_ai
                              ELSE '[]'::jsonb END) AS doc
            WHERE i.serv_id IN (:servIds)
              AND nullif(btrim(coalesce(doc ->> 'name', '')), '') IS NOT NULL
            """;

    /** 마감일 칸이 어떤 형식으로 들어오든 읽히도록 흔한 표기를 순서대로 시도한다. */
    private static final List<DateTimeFormatter> DATE_PATTERNS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"));

    private final EntityManager entityManager;

    /**
     * cb 테이블의 신청 마감일 / 결과 발표일 컬럼명. cb 스키마는 AI 서버 소유라 우리가 만들 수 없다.
     *
     * <p>비워두면(기본값) 그 칸을 아예 조회하지 않고 항상 null이 된다. 컬럼이 아직 없는데 SELECT에 넣으면
     * 찜 목록 조회까지 통째로 실패하므로, 상대 팀이 컬럼을 만든 뒤에 켜는 구조로 뒀다.
     * (application.yaml의 cb.deadline-column / cb.result-date-column에 실제 컬럼명을 적으면 켜진다)
     */
    private final String deadlineColumn;

    private final String resultDateColumn;

    private final String selectByServIds;

    public CbInstitutionReader(
            EntityManager entityManager,
            @Value("${cb.deadline-column:}") String deadlineColumn,
            @Value("${cb.result-date-column:}") String resultDateColumn) {
        this.entityManager = entityManager;
        this.deadlineColumn = normalize(deadlineColumn);
        this.resultDateColumn = normalize(resultDateColumn);

        StringBuilder columns = new StringBuilder(BASE_COLUMNS);
        if (this.deadlineColumn != null) {
            columns.append(", ").append(this.deadlineColumn);
        }
        if (this.resultDateColumn != null) {
            columns.append(", ").append(this.resultDateColumn);
        }
        this.selectByServIds = """
                SELECT %s
                FROM cb.cb_institutions
                WHERE serv_id IN (:servIds)
                """
                .formatted(columns);

        if (this.deadlineColumn == null) {
            log.info("cb 마감일 컬럼이 설정되지 않아 cb 제도의 마감 알림은 나가지 않습니다.");
        }
        if (this.resultDateColumn == null) {
            log.info("cb 결과 발표일 컬럼이 설정되지 않아 cb 제도의 발표일 알림은 나가지 않습니다.");
        }
    }

    private String normalize(String column) {
        return (column == null || column.isBlank()) ? null : column.trim();
    }

    /**
     * 제도 요약 1건.
     *
     * @param regionName 시도 + 시군구를 합친 표시용 지역명. 서울시 전체 사업이면 자치구가 비어 있다.
     * @param deadline 신청 마감일. cb.deadline-column을 설정하지 않았거나 값이 비었으면 null.
     * @param resultDate 결과 발표일. cb.result-date-column을 설정하지 않았거나 값이 비었으면 null.
     * @param documents 필요 서류. AI 서버가 제도별로 생성해 둔 값이라 우리 documents 테이블과는 연결되지 않는다.
     */
    public record CbInstitution(
            String servId, String name, String summary, String agencyName, String link, String regionName,
            boolean active, LocalDate deadline, LocalDate resultDate, List<CbDocument> documents) {}

    /**
     * cb 제도의 필요 서류 1건.
     *
     * <p>기존 제도의 서류는 documents 테이블의 행을 가리키지만, cb 서류는 AI가 제도마다 생성한 문자열이라
     * 이름 자체가 식별자다. url은 있을 때만 채워진다.
     *
     * @param urlType url의 성격. certificate_issuance(발급처) 또는 form_download(서식 내려받기).
     *     url이 없으면 null이다.
     */
    public record CbDocument(String name, String url, String urlType) {}

    public Optional<CbInstitution> findByServId(String servId) {
        return Optional.ofNullable(findAllByServIds(List.of(servId)).get(servId));
    }

    /** 찜 목록처럼 여러 건을 한 번에 그릴 때 쓴다. 항목마다 따로 조회하지 않으려는 것이다. */
    @Transactional(readOnly = true)
    public Map<String, CbInstitution> findAllByServIds(Collection<String> servIds) {
        if (servIds == null || servIds.isEmpty()) {
            return Map.of();
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
                .createNativeQuery(selectByServIds)
                .setParameter("servIds", servIds)
                .getResultList();

        Map<String, List<CbDocument>> documents = findDocumentsByServIds(servIds);

        return rows.stream()
                .map(row -> toInstitution(row, documents))
                .collect(Collectors.toMap(
                        CbInstitution::servId, Function.identity(), (first, second) -> first, LinkedHashMap::new));
    }

    /** 제도별 필요 서류를 한 번에 읽는다. (servId -> 서류 목록) 서류가 없는 제도는 키 자체가 없다. */
    private Map<String, List<CbDocument>> findDocumentsByServIds(Collection<String> servIds) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager
                .createNativeQuery(SELECT_DOCUMENTS)
                .setParameter("servIds", servIds)
                .getResultList();

        Map<String, List<CbDocument>> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.computeIfAbsent(asString(row[0]), key -> new ArrayList<>())
                    .add(new CbDocument(asString(row[1]).trim(), asString(row[2]), asString(row[3])));
        }
        return result;
    }

    /**
     * 선택 컬럼(마감일·발표일)은 설정된 것만 SELECT에 붙기 때문에, 붙은 순서대로 자리를 세어 읽는다.
     * 하나만 켜져 있으면 그게 8번 자리에 온다.
     */
    private CbInstitution toInstitution(Object[] row, Map<String, List<CbDocument>> documentsByServId) {
        int next = BASE_COLUMN_COUNT;
        LocalDate deadline = deadlineColumn == null ? null : toLocalDate(row[next++]);
        LocalDate resultDate = resultDateColumn == null ? null : toLocalDate(row[next]);

        String servId = asString(row[0]);
        return new CbInstitution(
                servId,
                asString(row[1]),
                asString(row[2]),
                asString(row[3]),
                asString(row[4]),
                toRegionName(asString(row[5]), asString(row[6])),
                row[7] == null || (Boolean) row[7],
                deadline,
                resultDate,
                documentsByServId.getOrDefault(servId, List.of()));
    }

    /** 예: "서울특별시 강남구", 자치구가 없으면 "서울특별시". */
    private String toRegionName(String ctpvNm, String sggNm) {
        if (ctpvNm == null || ctpvNm.isBlank()) {
            return sggNm;
        }
        return (sggNm == null || sggNm.isBlank()) ? ctpvNm : ctpvNm + " " + sggNm;
    }

    /**
     * 마감일 칸을 날짜로 바꾼다.
     *
     * <p>이 칸은 AI 서버가 채우는 것이라 date로 올지 '2026.8.14.' 같은 문자열로 올지 우리가 정할 수 없다.
     * 어느 쪽이 와도 읽히게 해두고, 형식을 못 알아보면 그 한 건만 null로 두고 넘어간다.
     * 알림 한 건이 안 나가는 것보다 배치 전체가 죽는 쪽이 훨씬 나쁘기 때문이다.
     */
    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }

        // 끝에 붙은 마침표("2026.8.14.")와 공백을 떼고 흔한 표기들을 차례로 시도한다.
        String text = value.toString().trim().replaceAll("[.\\s]+$", "");
        if (text.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_PATTERNS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // 다음 형식으로 계속 시도
            }
        }
        log.warn("cb 마감일 형식을 알아볼 수 없어 건너뜁니다. value={}", text);
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
