package com.globalbuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.globalbuddy")
@EntityScan(basePackages = "com.globalbuddy.model")
@EnableJpaRepositories(basePackages = "com.globalbuddy.repository")
@EnableScheduling
public class GlobalBuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(GlobalBuddyApplication.class, args);
    }
}

