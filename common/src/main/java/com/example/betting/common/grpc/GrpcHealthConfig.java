package com.example.betting.common.grpc;

import io.grpc.BindableService;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 모든 gRPC 서비스가 공유하는 표준 헬스체크 설정.
 * 각 서비스는 애플리케이션 클래스에서 {@code @Import(GrpcHealthConfig.class)} 로 가져온다.
 * Spring gRPC는 컨텍스트의 {@link BindableService} 빈을 gRPC 서버에 자동 등록한다.
 *
 * <p>component-scan 되는 @Configuration 이므로 Spring gRPC 서버 자동 구성이 이 헬스
 * {@link BindableService} 를 확실히 감지한다(@AutoConfiguration 순서 문제 회피).
 * 리플렉션(grpcurl 탐색용)은 Spring gRPC가 자체 자동 구성한다.
 */
@Configuration
public class GrpcHealthConfig {

    @Bean
    HealthStatusManager healthStatusManager() {
        HealthStatusManager manager = new HealthStatusManager();
        manager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, ServingStatus.SERVING);
        return manager;
    }

    @Bean
    BindableService grpcHealthService(HealthStatusManager healthStatusManager) {
        return healthStatusManager.getHealthService();
    }
}
