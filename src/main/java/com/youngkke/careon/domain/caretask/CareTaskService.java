package com.youngkke.careon.domain.caretask;

import com.youngkke.careon.domain.carer.Cared;
import com.youngkke.careon.domain.carer.CaredRepository;
import com.youngkke.careon.domain.carer.Carer;
import com.youngkke.careon.domain.carer.CarerRepository;
import com.youngkke.careon.domain.caretask.dto.CareTaskCompleteRequest;
import com.youngkke.careon.domain.caretask.dto.CareTaskCompleteResponse;
import com.youngkke.careon.domain.caretask.dto.CareTaskResponse;
import com.youngkke.careon.domain.caretask.dto.CareTaskTodayResponse;
import com.youngkke.careon.domain.caretask.dto.CareTaskUpsertRequest;
import com.youngkke.careon.domain.wear.WearDevice;
import com.youngkke.careon.domain.wear.WearDeviceRepository;
import com.youngkke.careon.global.error.BusinessException;
import com.youngkke.careon.global.error.ErrorCode;
import com.youngkke.careon.global.util.DateTimes;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 워치의 정기 안부·복약·할 일. 보호자가 등록하고 돌봄 대상자가 워치에서 체크한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareTaskService {

    private final CarerRepository carerRepository;
    private final CaredRepository caredRepository;
    private final WearDeviceRepository wearDeviceRepository;
    private final CareTaskRepository careTaskRepository;
    private final CareTaskCompletionRepository careTaskCompletionRepository;

    // ===== 워치 =====

    /** 워치에서 보는 오늘의 할 일. 예정 시각 순. */
    public List<CareTaskTodayResponse> listToday(Integer wearDeviceId) {
        WearDevice wearDevice = wearDeviceRepository.getConnectedOrThrow(wearDeviceId);
        LocalDate today = DateTimes.today();

        List<CareTask> tasks =
                careTaskRepository.findAllByCaredAndActiveIsTrueOrderByScheduledTimeAscCareTaskIdAsc(
                                wearDevice.getCared())
                        .stream()
                        .filter(task -> task.isScheduledOn(today))
                        .toList();
        if (tasks.isEmpty()) {
            return List.of();
        }

        // 항목마다 완료 여부를 따로 조회하지 않도록 한 번에 읽어 맞춘다.
        Map<Integer, CareTaskCompletion> completions =
                careTaskCompletionRepository.findAllByCareTaskInAndCompletedDate(tasks, today).stream()
                        .collect(Collectors.toMap(
                                completion -> completion.getCareTask().getCareTaskId(), Function.identity()));

        return tasks.stream()
                .map(task -> new CareTaskTodayResponse(
                        task.getCareTaskId(),
                        task.getTitle(),
                        DateTimes.toIsoString(today.atTime(task.getScheduledTime())),
                        completions.containsKey(task.getCareTaskId()),
                        task.getKind()))
                .toList();
    }

    /**
     * 워치에서 오늘 몫을 체크하거나 해제한다.
     * 완료는 날짜별로 남기므로, 어제 것을 오늘 고칠 수는 없고 항상 오늘 날짜에만 적용된다.
     */
    @Transactional
    public CareTaskCompleteResponse updateCompletion(
            Integer wearDeviceId, Integer taskId, CareTaskCompleteRequest request) {
        WearDevice wearDevice = wearDeviceRepository.getConnectedOrThrow(wearDeviceId);
        CareTask task = careTaskRepository
                .findById(taskId)
                .filter(t -> t.getCared().getCaredId().equals(wearDevice.getCared().getCaredId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_TASK_NOT_FOUND));

        LocalDate today = DateTimes.today();
        if (!request.completed()) {
            careTaskCompletionRepository.deleteByCareTaskAndCompletedDate(task, today);
            return new CareTaskCompleteResponse(task.getCareTaskId(), false, null);
        }

        LocalDateTime completedAt = request.completedAt() == null
                ? LocalDateTime.now()
                : DateTimes.parseToKstNotFuture(request.completedAt());
        CareTaskCompletion completion = careTaskCompletionRepository
                .findByCareTaskAndCompletedDate(task, today)
                .map(existing -> {
                    existing.updateCompletedAt(completedAt);
                    return existing;
                })
                .orElseGet(() -> careTaskCompletionRepository.save(CareTaskCompletion.builder()
                        .careTask(task)
                        .completedDate(today)
                        .completedAt(completedAt)
                        .build()));

        return new CareTaskCompleteResponse(
                task.getCareTaskId(), true, DateTimes.toIsoString(completion.getCompletedAt()));
    }

    // ===== 보호자 앱 =====

    public List<CareTaskResponse> list(Integer carerId, Integer caredId) {
        Cared cared = getOwnedCaredOrThrow(carerId, caredId);
        return careTaskRepository.findAllByCaredOrderByScheduledTimeAscCareTaskIdAsc(cared).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CareTaskResponse create(Integer carerId, Integer caredId, CareTaskUpsertRequest request) {
        Cared cared = getOwnedCaredOrThrow(carerId, caredId);
        validateForCreate(request);

        CareTask task = careTaskRepository.save(CareTask.builder()
                .cared(cared)
                .title(request.title())
                .kind(request.kind())
                .repeatType(request.repeatType())
                .scheduledTime(request.scheduledTime())
                .scheduledDate(request.repeatType() == CareTaskRepeat.ONCE ? request.scheduledDate() : null)
                .daysOfWeek(
                        request.repeatType() == CareTaskRepeat.WEEKLY
                                ? EnumSet.copyOf(request.daysOfWeek())
                                : EnumSet.noneOf(DayOfWeek.class))
                .active(request.active() == null || request.active())
                .build());
        return toResponse(task);
    }

    @Transactional
    public CareTaskResponse update(Integer carerId, Integer taskId, CareTaskUpsertRequest request) {
        CareTask task = getOwnedTaskOrThrow(carerId, taskId);
        task.update(
                request.title(),
                request.kind(),
                request.repeatType(),
                request.scheduledTime(),
                request.scheduledDate(),
                request.daysOfWeek(),
                request.active());
        return toResponse(task);
    }

    /**
     * 보호자가 할 일을 지운다. 행을 실제로 지우지 않고 끄기만 하는 이유는, 지금까지의 완료 기록이 함께
     * 사라지면 "지난주에 약을 먹었는지"를 되돌아볼 수 없기 때문이다.
     */
    @Transactional
    public void delete(Integer carerId, Integer taskId) {
        getOwnedTaskOrThrow(carerId, taskId).deactivate();
    }

    private void validateForCreate(CareTaskUpsertRequest request) {
        if (request.title() == null
                || request.title().isBlank()
                || request.kind() == null
                || request.repeatType() == null
                || request.scheduledTime() == null) {
            throw new BusinessException(ErrorCode.MISSING_INPUT_VALUE);
        }
        // 날짜나 요일이 비어 있으면 어느 날 목록에도 뜨지 않아 등록한 의미가 없다.
        if (request.repeatType() == CareTaskRepeat.ONCE && request.scheduledDate() == null) {
            throw new BusinessException(ErrorCode.MISSING_INPUT_VALUE);
        }
        if (request.repeatType() == CareTaskRepeat.WEEKLY
                && (request.daysOfWeek() == null || request.daysOfWeek().isEmpty())) {
            throw new BusinessException(ErrorCode.MISSING_INPUT_VALUE);
        }
    }

    private CareTaskResponse toResponse(CareTask task) {
        return new CareTaskResponse(
                task.getCareTaskId(),
                task.getTitle(),
                task.getKind(),
                task.getRepeatType(),
                task.getScheduledTime(),
                task.getScheduledDate(),
                task.getDaysOfWeek(),
                task.isActive());
    }

    /** 남의 할 일을 요청한 경우에도 존재 여부가 드러나지 않도록 404로 답한다. */
    private CareTask getOwnedTaskOrThrow(Integer carerId, Integer taskId) {
        return careTaskRepository
                .findById(taskId)
                .filter(task -> task.getCared().getCarer().getCarerId().equals(carerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CARE_TASK_NOT_FOUND));
    }

    private Cared getOwnedCaredOrThrow(Integer carerId, Integer caredId) {
        Carer carer = carerRepository.findById(carerId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return caredRepository
                .findByCaredIdAndCarer(caredId, carer)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARED_NOT_FOUND));
    }
}
