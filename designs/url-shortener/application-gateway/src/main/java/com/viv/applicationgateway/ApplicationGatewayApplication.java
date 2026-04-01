package com.viv.applicationgateway;

import com.viv.applicationgateway.config.EdgeSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(EdgeSecurityProperties.class)
public class ApplicationGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationGatewayApplication.class, args);
    }
}
