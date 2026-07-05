package com.example.betting.betting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.example.betting.common.grpc.GrpcHealthConfig;

@SpringBootApplication
@Import(GrpcHealthConfig.class)
public class BettingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BettingApplication.class, args);
    }
}
