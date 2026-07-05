# betting-system

학습용 스포츠 베팅 시스템 — Spring Boot 4.1 + gRPC + Kafka 기반.
배팅 + 위험관리(리스크) + 운영(관측성)을 여러 서비스로 나눠 gRPC(동기)와 Kafka(비동기)로 통신한다.

- 설계: [docs/superpowers/specs/2026-07-05-sports-betting-system-design.md](docs/superpowers/specs/2026-07-05-sports-betting-system-design.md)
- 구현 계획: [docs/superpowers/plans/2026-07-05-sports-betting-system-plan.md](docs/superpowers/plans/2026-07-05-sports-betting-system-plan.md)

## 기술 스택

Java 25 · Spring Boot 4.1 · Spring gRPC 1.x · Gradle(Kotlin DSL) 멀티모듈 · Kafka(KRaft) · Postgres(DB per service) · Prometheus/Grafana/Jaeger/OpenTelemetry.

## 모듈

| 모듈 | 역할 | gRPC 포트 | HTTP(Actuator) 포트 |
|------|------|:--------:|:-------------------:|
| `proto` | 공용 .proto + 코드 생성 | — | — |
| `common` | 공용 gRPC 설정(헬스 등) | — | — |
| `service-event` | Event/Odds | 9091 | 8081 |
| `service-betting` | Betting | 9092 | 8082 |
| `service-risk` | Risk | 9093 | 8083 |
| `service-wallet` | Wallet | 9094 | 8084 |
| `service-ops` | Ops/Admin | 9095 | 8085 |
| `service-bff` | BFF(REST + gRPC 클라이언트) | — | 8086 |

## 인프라 (docker-compose)

| 서비스 | 호스트 포트 | 비고 |
|--------|:----------:|------|
| Postgres | **15432** | 호스트 5432 점유 회피 (`event_db`/`betting_db`/`risk_db`/`wallet_db`/`ops_db`) |
| Kafka (KRaft) | **19092** | 호스트 클라이언트: `localhost:19092` |
| Prometheus | 9090 | 서비스를 `host.docker.internal:808x` 로 스크레이프 |
| Grafana | **3001** | 호스트 3000 점유 회피 (admin/admin) |
| Jaeger UI | 16686 | 분산추적 |
| OTel Collector | 4317/4318 | OTLP 수신 → Jaeger 전달 |

> 포트가 문서 기본값과 다른 것은 로컬 호스트 포트 충돌 회피 때문이다(Postgres 15432, Grafana 3001, BFF 8086).

## 실행

### 1) 인프라 기동

```bash
docker compose up -d
docker compose ps          # 전체 상태 확인
```

### 2) 서비스 실행 (Java 25 필요)

개별 실행:

```bash
./gradlew :service-event:bootRun
```

전체 빌드 후 실행:

```bash
./gradlew build
# 각 서비스: java -jar service-<name>/build/libs/service-<name>-0.0.1-SNAPSHOT.jar
```

### 3) 확인

```bash
# gRPC 헬스체크 (event=9091 … ops=9095)
grpcurl -plaintext -d '{}' localhost:9091 grpc.health.v1.Health/Check
grpcurl -plaintext localhost:9091 list          # 리플렉션으로 서비스 목록

# 관측성 UI
open http://localhost:9090      # Prometheus (Targets: 6개 서비스 UP)
open http://localhost:3001      # Grafana
open http://localhost:16686     # Jaeger
```

## 구현 진행

- **Phase 0 (완료):** 모노레포 + 인프라 + 빈 서비스 6개(gRPC 헬스/리플렉션). 통신·관측성 뼈대.
- Phase 1~7: 스펙의 구현 순서 참고 (Event → Wallet → Betting → BFF+화면 → Risk → Ops).
  비즈니스는 "뼈대 우선" 전략에 따라 처음엔 단순하게, 이후 각 서비스 내부만 확장.
