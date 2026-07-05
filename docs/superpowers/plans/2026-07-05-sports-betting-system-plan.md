# 스포츠 베팅 시스템 (학습용) — 구현 계획

- **작성일:** 2026-07-05
- **스펙:** `docs/superpowers/specs/2026-07-05-sports-betting-system-design.md`
- **원칙:** 각 Phase는 독립적으로 **검증 가능한 목표**를 가진다. 앞 Phase가 초록불이어야 다음으로 넘어간다. 학습 목적이므로 한 번에 한 Phase씩, 실제로 돌려보고 확인한다.

## 구현 전략: 뼈대 우선 (2-웨이브)

비즈니스 로직은 처음엔 **최대한 단순하게(스텁 수준)** 두고, 먼저 **구조 뼈대**(서비스 분리·gRPC/Kafka 통신·DB·관측성)가 end-to-end로 도는 것을 완성한다. 그 다음 상세 비즈니스를 채운다.

- **웨이브 1 — 뼈대 (Phase 0~6):** 모든 서비스가 뜨고, 통신 경로(동기 gRPC + 비동기 Kafka)가 실제로 연결되며, Jaeger/Grafana에 흐름이 보인다. 이 단계의 비즈니스는 **의도적으로 단순**하다:
  - 배당 = 고정값 또는 단순 랜덤 흔들기 (정교한 배당 계산 없음)
  - 정산 = 단순 승/패 판정 (핸디캡·무승부·다중 마켓 없음)
  - 리스크 = 단일 하드 한도(예: 사용자별 최대 스테이크) + 단순 임계 이상탐지
  - 지급 = 스테이크 × 배당의 단순 계산
- **웨이브 2 — 상세 비즈니스 (후속):** 뼈대가 초록불이 된 뒤 각 서비스 내부만 교체·확장한다. 인터페이스(proto)·통신 구조는 그대로 두고 로직만 깊게 만든다. 예: 다중 마켓/핸디캡, 배당 산출 모델, 익스포저 기반 실시간 리스크, 정교한 이상탐지 규칙 등.

> 핵심: 웨이브 2는 **서비스 경계와 통신을 바꾸지 않는다.** 뼈대를 잘 잡으면 비즈니스는 각 서비스 안에서만 자란다. 그래서 웨이브 1에서 proto·이벤트 스키마·경계를 신중히, 로직은 대충 잡는다.

## 공통 규약

- **버전:** Java 25, Spring Boot 4.1, Gradle(Kotlin DSL) 멀티모듈. 버전은 루트 `gradle/libs.versions.toml`(버전 카탈로그)에서 단일 관리.
- **포트 규약:** 각 서비스 gRPC 포트 `909x`, 관리(Actuator/HTTP) 포트 `808x`.
  - event=9091/8081, betting=9092/8082, risk=9093/8083, wallet=9094/8084, ops=9095/8085, bff=HTTP 8080.
- **proto:** 모든 `.proto`는 `proto/` 모듈 한 곳. 서비스는 이 모듈을 의존해 생성 스텁 공유.
- **DB per service:** Postgres 인스턴스는 하나지만 서비스별 **독립 데이터베이스**(event_db, betting_db, …)로 분리. 서비스는 자기 DB만 접근.
- **Kafka 토픽:** 이벤트 이름 = 토픽 이름(`OddsChanged`, `EventSettled`, `BetPlaced`, `BetSettled`, `WalletChanged`, `RiskAlert`, `OddsAdjustment`). 직렬화는 JSON(학습 가독성 우선; 후에 protobuf 전환 가능).
- **관측성:** 모든 서비스는 처음부터 Actuator + Micrometer(Prometheus) + OTel 자동계측을 켠 상태로 만든다. 대시보드/추적 확인은 Phase가 진행되며 자연히 채워진다.
- **검증 원칙:** 각 Phase 끝에 (1) 빌드/테스트 통과, (2) `docker compose up` 상태에서 실제 gRPC/REST 호출로 동작 확인, (3) 해당되면 Jaeger에서 trace 연결 확인.

---

## Phase 0 — 모노레포 + 인프라 스캐폴딩

**목표:** 빈 서비스들이 뜨고, `docker compose up`으로 인프라 전체가 기동되며, gRPC 헬스체크가 응답한다.

**작업:**
1. Gradle 멀티모듈 루트 구성 — `settings.gradle.kts`(모듈 8개 포함), 루트 `build.gradle.kts`(공통 플러그인: java toolchain 25, spring boot, protobuf), `gradle/libs.versions.toml`.
2. `proto/` 모듈 — protobuf 플러그인 + gRPC 코드생성 설정. 우선 `health`/공용 타입만.
3. `common/` 모듈 — 공용 에러 매핑, gRPC 인터셉터(로깅/추적 컨텍스트) 스켈레톤.
4. 서비스 6개(`service-event/betting/risk/wallet/ops/bff`) — 최소 Spring Boot 앱 + Spring gRPC 서버 기동(빈 서비스), Actuator 노출.
5. `docker-compose.yml` — postgres(다중 DB init 스크립트), kafka(KRaft), prometheus, grafana, jaeger, otel-collector. `prometheus.yml`/`otel-collector.yaml` 설정 포함.
6. 루트 README에 실행법 정리.

**검증:**
- `./gradlew build` 성공.
- `docker compose up -d` 후 postgres/kafka/prometheus/grafana/jaeger/otel 모두 healthy.
- 각 서비스 기동 후 `grpc_health_probe` 또는 grpcurl로 헬스체크 `SERVING` 응답.
- Prometheus에서 각 서비스 `/actuator/prometheus` 타깃 UP.

---

## Phase 1 — Event/Odds 서비스

**목표:** 경기·마켓·배당을 제공하고, 배당 변동을 스트리밍/이벤트로 내보낸다.

**작업:**
1. proto: `event.proto` — `ListEvents`, `GetOdds`, `StreamOdds`(server-streaming).
2. 도메인/저장: 경기(Event), 마켓(Market), 배당(Odds) 엔티티 + Postgres(event_db) 스키마(Flyway).
3. 시드 데이터: 학습용 경기 몇 개 + 마켓/배당.
4. `StreamOdds` 구현: 구독 클라이언트에게 배당 변동 푸시(간단한 인메모리 스케줄러로 배당 흔들기 또는 수동 트리거).
5. Kafka 발행: `OddsChanged`, (경기 종료 트리거 시)`EventSettled`.
6. Kafka 구독: `OddsAdjustment`(Risk 신호) → 배당 조정. (Phase 5 전엔 소비자만 준비, no-op 허용)

**검증:**
- grpcurl로 `ListEvents`/`GetOdds` 정상.
- `StreamOdds` 구독 시 배당 변동 스트림 수신.
- `OddsChanged` 토픽에 메시지 발행 확인(kafka-console-consumer).

---

## Phase 2 — Wallet 서비스

**목표:** 가상 잔액 차감/지급이 정합적으로 동작하고 원장이 남는다.

**작업:**
1. proto: `wallet.proto` — `Debit`, `Credit`, `GetBalance`.
2. 도메인/저장: 계정(Account) 잔액 + 원장(LedgerEntry), Postgres(wallet_db) + Flyway.
3. 정합성: `Debit`은 잔액 부족 시 실패(gRPC `FAILED_PRECONDITION`). 멱등키로 중복 요청 방지.
4. Kafka 구독: `BetSettled` → 당첨금 `Credit`. 멱등 소비.
5. Kafka 발행: `WalletChanged`.

**검증:**
- Debit/Credit 후 `GetBalance` 정합, 원장 기록 확인.
- 잔액 부족 Debit이 올바른 gRPC 에러로 거절.
- 동일 멱등키 재요청 시 이중 처리 안 됨(단위/통합 테스트).

---

## Phase 3 — Betting 서비스 (동기 오케스트레이터)

**목표:** `PlaceBet` 전체 흐름이 성공하고, Betting→Risk/Wallet 호출이 하나의 trace로 이어진다.

**작업:**
1. proto: `betting.proto` — `PlaceBet`, `GetBet`, `ListBets`.
2. 도메인/저장: 베팅 슬립(BetSlip) + 상태(PENDING/ACCEPTED/WON/LOST/VOID), Postgres(betting_db) + Flyway.
3. 오케스트레이션: `PlaceBet` 시 Risk `CheckBet`(gRPC) → Wallet `Debit`(gRPC) → 저장(PENDING) → `BetPlaced` 발행.
   - Risk 거절/Wallet 실패 시 거절 응답. Debit 성공 후 저장 실패 시 보상(Wallet `Credit` 롤백).
   - Phase 5 전에는 Risk가 없으므로 `CheckBet`은 스텁(항상 승인) 또는 feature flag로 우회.
4. Kafka 구독: `EventSettled` → 승/패 판정 → 상태 갱신 → `BetSettled` 발행.

**검증:**
- BFF 없이 grpcurl `PlaceBet`로 성공/거절 경로 확인.
- `EventSettled` 발행 시 관련 베팅 정산 → `BetSettled` 발행 확인.
- **Jaeger에서 PlaceBet → Debit이 단일 trace로 연결** 확인.

---

## Phase 4 — BFF + 정적 화면

**목표:** 브라우저에서 경기 조회·베팅·잔액 조회가 되고, Browser→BFF→백엔드가 하나의 trace로 이어진다.

**작업:**
1. `service-bff` REST 컨트롤러: `GET /api/events`, `POST /api/bets`, `GET /api/bets`, `GET /api/wallet/{userId}`.
2. gRPC 클라이언트: Event/Betting/Wallet 스텁 주입, 응답 조합(예: 베팅 목록 + 경기명 조인).
3. 정적 화면: `src/main/resources/static/`에 `index.html` + 바닐라 JS(fetch). 경기 목록 / 베팅 폼 / 내 베팅·잔액.
4. OTel: HTTP 인입 → gRPC 아웃바운드 trace 전파 확인.

**검증:**
- 브라우저에서 경기 목록 표시 → 베팅 걸기 → 잔액/내 베팅 갱신.
- **Jaeger에서 Browser(HTTP)→BFF→Betting→Wallet 단일 trace** 확인.

---

## Phase 5 — Risk 서비스 (혼합)

**목표:** 하드 한도는 동기로 거절, 이상탐지는 비동기로 알림/배당조정.

**작업:**
1. proto: `risk.proto` — `CheckBet`(승인/거절), `GetExposure`.
2. 동기 게이트: `CheckBet`에서 사용자·마켓·이벤트별 하드 한도(최대 익스포저) 검사. Betting의 스텁을 실제 호출로 교체.
3. 저장: 한도 설정 + 익스포저 누적, Postgres(risk_db) + Flyway.
4. 비동기 감시: `BetPlaced` 구독 → 익스포저 집계 + 이상 패턴 탐지 → `RiskAlert`(Ops용) / `OddsAdjustment`(Event/Odds용) 발행.
5. Event/Odds의 `OddsAdjustment` 소비를 실제 배당 조정으로 연결(Phase 1의 no-op 대체).

**검증:**
- 하드 한도 초과 베팅이 `PlaceBet`에서 즉시 거절.
- `BetPlaced` 누적으로 이상 감지 시 `RiskAlert`/`OddsAdjustment` 발행 → Event/Odds 배당 조정 확인.
- `GetExposure`로 누적 익스포저 조회 정합.

---

## Phase 6 — Ops/Admin + Grafana 대시보드

**목표:** 시스템 상태를 한눈에 보고, 핵심 메트릭이 대시보드로 보인다.

**작업:**
1. proto: `ops.proto` — `GetSystemStatus`, `ListAlerts`.
2. 집계: 전 이벤트(`BetPlaced/BetSettled/WalletChanged/RiskAlert`) 구독 → 통계/알림 스냅샷, Postgres(ops_db).
3. `GetSystemStatus`: 서비스 헬스 집계 + 주요 카운터.
4. Grafana 대시보드(JSON provisioning): 베팅 처리량, Risk 거절율, Wallet 실패율, gRPC 지연 분포.

**검증:**
- `GetSystemStatus`/`ListAlerts` 조회 정상.
- Grafana에서 4개 핵심 패널이 실데이터로 채워짐.
- 부하를 조금 준 뒤 대시보드·Jaeger·알림이 일관되게 반응.

---

## 리스크 & 메모

- **Spring Boot 4.1 + Spring gRPC + Java 25** 조합의 스타터/플러그인 버전 호환은 Phase 0에서 가장 먼저 확정(여기서 막히면 뒤 전부 막힘).
- Kafka 직렬화는 JSON으로 시작(가독성) → 필요 시 protobuf 전환은 후속.
- 정확히-한 번 시맨틱은 도입하지 않음: at-least-once + **멱등 소비**로 처리(Wallet Credit, Betting 정산에 멱등키 필수).
- 각 Phase는 커밋 단위로 남기고, 검증 로그(gRPC 호출 결과/Jaeger 스크린샷 등)를 짧게 기록.
