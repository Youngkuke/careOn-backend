package com.youngkke.careon.global.dto;

/** 단순 성공 메시지만 반환하는 API들의 공통 응답 body. */
public record MessageResponse(String message) {
}
