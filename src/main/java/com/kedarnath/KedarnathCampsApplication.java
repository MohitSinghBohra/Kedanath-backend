package com.kedarnath;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KedarnathCampsApplication {
    public static void main(String[] args) {
        SpringApplication.run(KedarnathCampsApplication.class, args);
    }
}
