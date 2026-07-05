plugins {
    `java-library`
}

dependencies {
    api(platform(libs.spring.grpc.bom))
    api(project(":proto"))
    api(libs.grpc.services) // health service impl shared by all gRPC services

    // @AutoConfiguration / @Bean 를 컴파일하기 위한 최소 의존성 (런타임은 서비스가 스타터로 제공)
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:${libs.versions.springBoot.get()}")
}
