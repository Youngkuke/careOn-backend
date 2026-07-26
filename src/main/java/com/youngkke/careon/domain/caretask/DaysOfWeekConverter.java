package com.youngkke.careon.domain.caretask;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 반복 요일을 한 컬럼에 "MONDAY,WEDNESDAY,FRIDAY" 형태로 저장한다.
 *
 * <p>요일은 최대 7개뿐이고 이 값으로 검색할 일도 없어서, 별도 테이블(@ElementCollection)을 두는 것보다
 * 한 컬럼에 담는 편이 단순하다. 도메인에서는 Set&lt;DayOfWeek&gt;로 다루고, DB에는 사람이 읽을 수 있는
 * 문자열로 남도록 요일 순서대로 정렬해 저장한다.
 */
@Converter
public class DaysOfWeekConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return null;
        }
        return daysOfWeek.stream().sorted().map(Enum::name).collect(Collectors.joining(DELIMITER));
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return Arrays.stream(value.split(DELIMITER))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }
}
