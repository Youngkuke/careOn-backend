package com.youngkke.careon.global.config;

/*
 * snake_case JSON 설정은 Jackson 라이브러리 버전(Spring Boot 4의 Jackson 3 등)에 따라
 * 패키지 경로가 달라져서, Java 설정 대신 src/main/resources/jackson.properties에서
 * spring.jackson.property-naming-strategy=SNAKE_CASE 로 지정한다.
 * (CareonApplication의 @PropertySource로 로딩됨)
 *
 * 이 파일은 설정 위치를 찾기 쉽도록 남겨둔 안내용 주석 파일이다.
 */
final class JacksonConfig {

    private JacksonConfig() {}
}
