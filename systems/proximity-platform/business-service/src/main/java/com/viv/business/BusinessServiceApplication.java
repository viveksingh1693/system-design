package com.viv.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BusinessServiceApplication {

	private static final Logger log = LoggerFactory.getLogger(BusinessServiceApplication.class);

	public static void main(String[] args) {
		System.setProperty("user.timezone", "Asia/Kolkata");

		ConfigurableApplicationContext context = SpringApplication.run(BusinessServiceApplication.class, args);

		String appName = context.getEnvironment().getProperty("spring.application.name", "unknown");
		String otlpEndpoint = context.getEnvironment().getProperty("management.otlp.tracing.endpoint", "not-configured");
		String samplingProbability = context.getEnvironment().getProperty("management.tracing.sampling.probability", "0");

		log.info("========================================");
		log.info("Application '{}' started.", appName);
		log.info("Tracing sampling probability: {}", samplingProbability);
		log.info("OTLP tracing endpoint: {}", otlpEndpoint);
		log.info("Jaeger trace export target should be: {}", otlpEndpoint);
		log.info("========================================");
	}

}
