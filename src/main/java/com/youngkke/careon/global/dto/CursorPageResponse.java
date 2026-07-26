package com.youngkke.careon.global.dto;

import java.util.List;

/**
 * 커서 페이징 목록의 공통 응답.
 *
 * @param nextCursor 다음 페이지 요청에 그대로 넘길 값. null이면 마지막 페이지다.
 */
public record CursorPageResponse<T>(List<T> items, String nextCursor) {}
