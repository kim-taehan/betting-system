plugins {
    `java-library`
}

// 서비스가 공유하는 proto 스텁 + gRPC 공통 런타임 + 분산추적 스택을 re-export.
dependencies {
    api(platform(libs.spring.boot.bom))
    api(platform(libs.spring.grpc.bom))

    api(project(":proto"))
    api(libs.grpc.services)

    // 분산추적: Micrometer Tracer 자동구성 + Observation→OTel 브리지 + OTLP 익스포터 + OTel SDK
    api(libs.spring.boot.micrometer.tracing)
    api(libs.micrometer.tracing.bridge.otel)
    api(libs.otel.exporter.otlp)
    api(libs.spring.boot.opentelemetry)

    // 공용 @AutoConfiguration(추적 익스포터) 컴파일용 (런타임은 서비스가 제공)
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
}
