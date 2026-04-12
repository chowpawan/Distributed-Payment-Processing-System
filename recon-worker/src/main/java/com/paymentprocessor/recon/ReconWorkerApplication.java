package com.paymentprocessor.recon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReconWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconWorkerApplication.class, args);
    }
}
