package com.youngkke.careon.domain.policy.dto;

import java.util.List;

/** 저장한 제도 목록 조회(앱) 응답 항목. 마감일 카드와 발표일 카드는 별도 항목으로 분리해서 내려준다. */
public record AppSavedPolicyResponse(
        String name, String deadline, String deadlineDDay, List<String> documents, String resultDate, String resultDDay) {}
