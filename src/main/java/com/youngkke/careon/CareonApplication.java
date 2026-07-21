package com.youngkke.careon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@PropertySource("classpath:jackson.properties")
public class CareonApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareonApplication.class, args);
	}

}
