package com.bose.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@EnableConfigServer
@SpringBootApplication
public class ConfigServiceAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceAppApplication.class, args);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 500)
    public CustomPropertyPathNotificationExtractor customPropertyPathNotificationExtractor() {
        return new CustomPropertyPathNotificationExtractor();
    }
}
