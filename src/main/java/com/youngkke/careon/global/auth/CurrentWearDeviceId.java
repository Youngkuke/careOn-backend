package com.youngkke.careon.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 붙이면, Authorization 헤더의 wearAccessToken을 검증하고 wearDeviceId를 주입해준다.
 * 예: public ResponseEntity<?> create(@CurrentWearDeviceId Integer wearDeviceId) { ... }
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentWearDeviceId {
}
