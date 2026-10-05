package com.shreyansh.regressionguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
@SpringBootApplication
@ConfigurationPropertiesScan
public class RegressionGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegressionGuardApplication.class, args);
    }
}
