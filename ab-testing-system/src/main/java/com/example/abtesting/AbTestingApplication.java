package com.example.abtesting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * A/B 实验分流压测系统
 */
@SpringBootApplication
@EnableScheduling
public class AbTestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbTestingApplication.class, args);
    }
}
