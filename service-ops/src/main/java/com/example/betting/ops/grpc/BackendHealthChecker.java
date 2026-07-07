package com.example.betting.ops.grpc;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

/** 백엔드 서비스들의 gRPC 헬스를 라이브로 조회해 상태를 집계한다. */
@Component
public class BackendHealthChecker {

    private final Map<String, HealthGrpc.HealthBlockingStub> stubs = new LinkedHashMap<>();

    public BackendHealthChecker(GrpcChannelFactory channels) {
        for (String name : List.of("event", "betting", "risk", "wallet")) {
            stubs.put(name, HealthGrpc.newBlockingStub(channels.createChannel(name)));
        }
    }

    public List<ServiceHealth> checkAll() {
        List<ServiceHealth> result = new ArrayList<>();
        stubs.forEach((name, stub) -> {
            String status;
            try {
                var response = stub.withDeadlineAfter(1, TimeUnit.SECONDS)
                        .check(HealthCheckRequest.getDefaultInstance());
                status = response.getStatus().name();   // SERVING / NOT_SERVING
            } catch (RuntimeException ex) {
                status = "NOT_SERVING";
            }
            result.add(new ServiceHealth(name + "-service", status));
        });
        result.add(new ServiceHealth("ops-service", "SERVING"));
        return result;
    }

    public record ServiceHealth(String name, String status) {
    }
}
