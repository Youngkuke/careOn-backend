package com.youngkke.careon;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@PropertySource("classpath:jackson.properties")
public class CareonApplication {

	public static void main(String[] args) {
		// 서버 OS 시간대(EC2는 기본 UTC)와 무관하게 LocalDateTime.now()와
		// Hibernate @CreationTimestamp/@UpdateTimestamp가 항상 KST 기준으로 동작하도록 고정한다.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(CareonApplication.class, args);
	}

}
