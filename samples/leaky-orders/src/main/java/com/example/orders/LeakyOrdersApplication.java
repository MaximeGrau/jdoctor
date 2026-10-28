package com.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A Spring Boot app with two bugs on purpose, for jdoctor demos and tests:
 * a cache that never evicts (memory leak) and a transaction that holds its connection
 * during a slow call (HikariCP pool saturation).
 */
@SpringBootApplication
public class LeakyOrdersApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeakyOrdersApplication.class, args);
    }
}
