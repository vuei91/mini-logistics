package com.cjlogistics.mini;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CjLogisticsMiniApplication {

	public static void main(String[] args) {
		SpringApplication.run(CjLogisticsMiniApplication.class, args);
	}

}
