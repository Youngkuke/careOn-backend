package com.youngkke.careon.global.util;

/** 목록 API가 공통으로 쓰는 페이지 크기 규칙. */
public final class Pagination {

    private static final int DEFAULT_LIMIT = 20;

    /** 클라이언트가 아주 큰 limit을 보내 한 번에 전부 긁어가지 않도록 두는 상한. */
    private static final int MAX_LIMIT = 100;

    private Pagination() {}

    /** limit이 없으면 기본값, 범위를 벗어나면 가까운 경계로 맞춘다. 값이 이상하다고 에러를 주기보다 목록을 보여주는 쪽이 낫다. */
    public static int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
