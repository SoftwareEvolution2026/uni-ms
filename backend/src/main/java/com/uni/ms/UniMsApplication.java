package com.uni.ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class UniMsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniMsApplication.class, args);
    }
}
