package com.viv.nearby;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NearbyServiceApplication {

	public static void main(String[] args) {
		System.setProperty("user.timezone", "UTC");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(NearbyServiceApplication.class, args);
	}

}
