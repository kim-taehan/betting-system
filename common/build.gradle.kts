plugins {
    `java-library`
}

// 서비스가 공유하는 proto 스텁 + gRPC 공통 런타임을 re-export 한다.
// (공용 인터셉터/에러 매핑 등 공유 코드는 이후 여기에 추가한다.)
dependencies {
    api(platform(libs.spring.grpc.bom))
    api(project(":proto"))
    api(libs.grpc.services)
}
