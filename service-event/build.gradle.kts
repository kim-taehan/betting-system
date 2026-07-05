plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(platform(libs.spring.grpc.bom))
    implementation(project(":common"))

    implementation(libs.spring.grpc.server.starter)

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // 영속성
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Boot 4.x는 autoconfig를 모듈로 분리 → Flyway 자동 구성은 spring-boot-flyway 모듈이 제공
    implementation("org.springframework.boot:spring-boot-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // 메시징 (Boot 4.x: spring-boot-kafka 모듈이 KafkaAutoConfiguration/KafkaTemplate 제공)
    implementation("org.springframework.boot:spring-boot-kafka")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.spring.grpc.test)
}
