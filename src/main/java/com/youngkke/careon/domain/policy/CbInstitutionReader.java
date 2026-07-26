package com.youngkke.careon.domain.policy;

import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CbInstitutionReader {

    private static final String SELECT_BY_SERV_IDS =
            """
            SELECT serv_id, serv_nm, serv_dgst, jur_org_nm, detail_link, ctpv_nm, sgg_nm, is_active
            FROM cb.cb_institutions
            WHERE serv_id IN (:servIds)
            """;

    private final EntityManager entityManager;

    /**
     * 제도 요약 1건.
     *
     * @param regionName 시도 + 시군구를 합친 표시용 지역명. 서울시 전체 사업이면 자치구가 비어 있다.
     */
    public record CbInstitution(
            String servId, String name, String summary, String agencyName, String link, String regionName,
            boolean active) {}

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
                .createNativeQuery(SELECT_BY_SERV_IDS)
                .setParameter("servIds", servIds)
                .getResultList();

        return rows.stream()
                .map(this::toInstitution)
                .collect(Collectors.toMap(
                        CbInstitution::servId, Function.identity(), (first, second) -> first, LinkedHashMap::new));
    }

    private CbInstitution toInstitution(Object[] row) {
        return new CbInstitution(
                asString(row[0]),
                asString(row[1]),
                asString(row[2]),
                asString(row[3]),
                asString(row[4]),
                toRegionName(asString(row[5]), asString(row[6])),
                row[7] == null || (Boolean) row[7]);
    }

    /** 예: "서울특별시 강남구", 자치구가 없으면 "서울특별시". */
    private String toRegionName(String ctpvNm, String sggNm) {
        if (ctpvNm == null || ctpvNm.isBlank()) {
            return sggNm;
        }
        return (sggNm == null || sggNm.isBlank()) ? ctpvNm : ctpvNm + " " + sggNm;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
