package com.example.betting.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.example.betting.common.grpc.GrpcHealthConfig;

@SpringBootApplication
@Import(GrpcHealthConfig.class)
public class RiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskApplication.class, args);
    }
}
