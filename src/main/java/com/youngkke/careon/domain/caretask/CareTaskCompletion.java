package com.youngkke.careon.domain.caretask;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 할 일을 어느 날 완료했는지. 매일 반복되는 항목이 날짜별로 따로 체크돼야 해서 별도 행으로 남긴다.
 * (care_task_id, completed_date) 유니크 제약으로 같은 날 두 번 기록되지 않게 한다.
 */
@Entity
@Table(
        name = "care_task_completion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"care_task_id", "completed_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CareTaskCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_task_completion_id")
    private Integer careTaskCompletionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_task_id", nullable = false)
    private CareTask careTask;

    /** 어느 날짜의 할 일인지 (KST 기준). */
    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    /** 실제로 완료한 시각. 워치가 오프라인에서 체크한 뒤 나중에 올릴 수 있어 서버 수신 시각과 다를 수 있다. */
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    public void updateCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
