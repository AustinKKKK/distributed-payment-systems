package com.example.tier3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class Tier3Application {

	public static void main(String[] args) {
		SpringApplication.run(Tier3Application.class, args);
	}

}
