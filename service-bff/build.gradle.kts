plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(platform(libs.spring.grpc.bom))
    // common: proto 스텁 + 분산추적 스택 (BFF 스팬도 trace 에 포함)
    implementation(project(":common"))

    implementation(libs.spring.grpc.client.starter)

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.spring.grpc.test)
}
