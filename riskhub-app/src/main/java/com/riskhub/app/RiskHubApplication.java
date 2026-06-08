package com.riskhub.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.riskhub")
@MapperScan("com.riskhub.store.mapper")
public class RiskHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskHubApplication.class, args);
    }
}
