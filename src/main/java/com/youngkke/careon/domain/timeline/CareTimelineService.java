package com.youngkke.careon.domain.timeline;

import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.timeline.dto.CareTimelineItemResponse;
import com.youngkke.careon.global.dto.CursorPageResponse;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.Cursors;
import com.youngkke.careon.global.util.DateTimes;
import com.youngkke.careon.global.util.Pagination;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SOS·이탈·워치 연결·위치 공유를 한 줄기로 보여주는 타임라인 조회. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareTimelineService {

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final CareEventRepository careEventRepository;

    /** 권한 없는 cared를 요청하면 CARED_NOT_FOUND(404)라, 그 번호에 기록이 있는지 자체가 드러나지 않는다. */
    public CursorPageResponse<CareTimelineItemResponse> list(
            Integer carerId, Integer caredId, String cursor, Integer limit) {
        Cared cared = getOwnedCaredOrThrow(carerId, caredId);
        int pageSize = Pagination.resolveLimit(limit);
        Cursors.Position position = Cursors.decode(cursor);

        // 다음 페이지가 있는지 판단하려고 한 건 더 읽는다. 별도 count 쿼리를 돌리지 않기 위해서다.
        Pageable pageable = Pageable.ofSize(pageSize + 1);
        List<CareEvent> found = position == null
                ? careEventRepository.findAllByCaredOrderByOccurredAtDescCareEventIdDesc(cared, pageable)
                : careEventRepository.findPageAfter(cared, position.timestamp(), position.id(), pageable);

        boolean hasNext = found.size() > pageSize;
        List<CareEvent> page = hasNext ? found.subList(0, pageSize) : found;
        String nextCursor = hasNext
                ? Cursors.encode(
                        page.get(page.size() - 1).getOccurredAt(),
                        page.get(page.size() - 1).getCareEventId())
                : null;

        return new CursorPageResponse<>(page.stream().map(this::toItemResponse).toList(), nextCursor);
    }

    private CareTimelineItemResponse toItemResponse(CareEvent event) {
        return new CareTimelineItemResponse(
                event.getType().getSource() + ":" + event.getCareEventId(),
                event.getType(),
                DateTimes.toIsoString(event.getOccurredAt()),
                event.getRefId(),
                event.getSummary());
    }

    private Cared getOwnedCaredOrThrow(Integer carerId, Integer caredId) {
        Carer carer = carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return caredRepository
                .findByCaredIdAndCarer(caredId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARED_NOT_FOUND));
    }
}
