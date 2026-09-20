package com.viv.business;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling 
public class BusinessServiceApplication {

	public static void main(String[] args) {

		//   TimeZone.setDefault(
        //     TimeZone.getTimeZone("Asia/Kolkata")
        // );

		System.setProperty("user.timezone", "Asia/Kolkata");
		SpringApplication.run(BusinessServiceApplication.class, args);
	}

}
