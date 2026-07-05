# 스포츠 베팅 시스템 (학습용) — 설계 문서

- **작성일:** 2026-07-05
- **상태:** 승인됨 (구현 계획 작성 대기)
- **목적:** 학습. 무게중심은 (A) gRPC 서비스 간 통신, (B) Spring Boot 4.1 신기능, (D) 분산 시스템 운영/관측성. 배팅 도메인 로직(C)은 이를 얹기 위한 재료로 다룬다.

## 1. 학습 목표와 범위

| 목표 | 이 프로젝트에서 다루는 방식 |
|------|--------------------------|
| A. gRPC 통신 | 5개 서비스를 gRPC로 분리. 동기 오케스트레이션 + server-streaming |
| B. Spring Boot 4.1 | Java 25, Spring gRPC(공식), virtual thread, Actuator/Micrometer |
| D. 분산 운영 | Kafka 이벤트 버스, OpenTelemetry 분산추적, Prometheus/Grafana |

**도메인:** 스포츠 베팅 (경기 결과에 베팅 → 경기 종료 시 정산). pari-mutuel 같은 실시간 배당 재계산은 범위 밖.

**화면/BFF:** 브라우저는 **BFF**(Backend-for-Frontend)에 HTTP/JSON으로 붙고, BFF가 백엔드 서비스들에게 **gRPC 클라이언트**로 통신·조합한다. 화면은 BFF가 직접 서빙하는 **정적 HTML + 바닐라 JS**로 최소 구성(경기 목록 / 베팅 걸기 / 내 베팅·잔액 조회). 별도 프론트엔드 빌드 툴체인 없음.

**범위 제외 (YAGNI):**
- 프론트엔드 프레임워크(React 등)·빌드 툴체인 — 정적 HTML/JS로 대체.
- gRPC-web / 별도 Gateway — 브라우저는 BFF의 REST만 사용(BFF가 곧 진입점).
- 로그 집계(Loki), 알림(Alertmanager) — 후속 확장.
- 인증/인가, 실제 결제 연동 — 학습 범위 밖(가상 잔액만).

## 2. 기술 스택

| 항목 | 선택 |
|------|------|
| 언어 | Java 25 (LTS) |
| 프레임워크 | Spring Boot 4.1 |
| 빌드 | Gradle (Kotlin DSL), 멀티모듈 모노레포 |
| RPC | gRPC + protobuf, Spring gRPC (`spring-grpc-spring-boot-starter`) |
| 메시징 | Kafka (KRaft 모드) |
| 저장소 | Postgres — DB per service (서비스당 독립 스키마/인스턴스) |
| 관측성 | Micrometer + Prometheus + Grafana, OpenTelemetry + OTel Collector + Jaeger |
| 테스트 | JUnit 5, Testcontainers (Postgres/Kafka), gRPC in-process 서버 |
| 인프라 | Docker Compose |

## 3. 아키텍처 개요

```
 Browser ──HTTP/JSON──▶ BFF (REST 컨트롤러 + gRPC 클라이언트, 정적 HTML/JS 서빙)
                          │ gRPC
     ┌─────────┬──────────┼────────────┬──────────┐
     ▼         ▼          ▼            ▼          ▼
 ┌────────┐┌────────┐ ┌────────┐  ┌────────┐ ┌────────┐
 │Event/  ││Betting │ │ Risk   │  │ Wallet │ │Ops/    │
 │Odds    ││        │ │        │  │        │ │Admin   │
 └───┬────┘└───┬────┘ └───┬────┘  └───┬────┘ └───┬────┘
     │ 각 서비스 = 독립 Postgres + 독립 배포                  │
     └──────────────── Kafka (이벤트 버스) ─────────────────┘
```

- **동기(gRPC):** 즉시 응답·정합성 필요 경로. 예) 베팅 접수 시 Betting → Risk → Wallet.
- **비동기(Kafka):** 전파·후속처리. 예) `BetPlaced`, `EventSettled`, `OddsChanged`, `BetSettled`.
- **DB per service:** 각 서비스는 자기 DB만 소유. 타 서비스 테이블 직접 접근 금지.

## 4. 모노레포 모듈 구조

```
betting-system/
├── settings.gradle.kts, build.gradle.kts        # 루트 (공통 플러그인/버전 카탈로그)
├── docker-compose.yml                           # postgres×5, kafka, prometheus, grafana, jaeger, otel-collector
├── proto/                                        # 공용: 모든 .proto + 코드 생성
├── common/                                       # 공용 유틸(에러 매핑, gRPC 인터셉터, 추적 설정)
├── service-bff/                                  # BFF: REST + gRPC 클라이언트 + 정적 HTML/JS
├── service-event/                               # Event/Odds
├── service-betting/
├── service-risk/
├── service-wallet/
└── service-ops/
```

## 5. 서비스별 책임 & 인터페이스

| 서비스 | 주요 인터페이스 | Kafka 발행 | Kafka 구독 | 소유 데이터 |
|--------|-----------|-----------|-----------|------------|
| **BFF** | REST: `GET /api/events`, `POST /api/bets`, `GET /api/bets`, `GET /api/wallet/{userId}` (→ 내부 gRPC 호출·조합) | — | — | 없음(무상태) |
| **Event/Odds** | `ListEvents`, `GetOdds`, `StreamOdds`(server-streaming) | `OddsChanged`, `EventSettled` | — | 경기, 마켓, 배당 |
| **Betting** | `PlaceBet`, `GetBet`, `ListBets` | `BetPlaced`, `BetSettled` | `EventSettled`(정산), `OddsChanged` | 베팅 슬립, 상태 |
| **Risk** | `CheckBet`(동기 승인/거절), `GetExposure` | `RiskAlert` | `BetPlaced`(익스포저 집계) | 한도, 익스포저 |
| **Wallet** | `Debit`, `Credit`, `GetBalance` | `WalletChanged` | `BetSettled`(당첨금 지급) | 계정 잔액, 원장 |
| **Ops/Admin** | `GetSystemStatus`, `ListAlerts` | — | 전 이벤트(집계) | 집계 스냅샷 |

**핵심 학습 포인트:**
- `StreamOdds` → gRPC server-streaming.
- `PlaceBet` 경로 → 동기 gRPC 오케스트레이션 + 분산추적.
- 정산 경로 → Kafka 기반 이벤트 전파(최종 일관성).

## 6. 핵심 데이터 흐름

**베팅 접수 (동기 오케스트레이션):**
```
Browser ─POST /api/bets─▶ BFF ─PlaceBet(gRPC)─▶ Betting
  Betting ─CheckBet(gRPC)─▶ Risk    (한도 초과 시 거절)
  Betting ─Debit(gRPC)────▶ Wallet  (잔액 부족/실패 시 거절, 필요 시 보상)
  Betting: 베팅 저장(PENDING) → Kafka `BetPlaced` 발행
◀─ BetSlip 응답 (Browser→BFF→Betting→Risk/Wallet 전 과정이 하나의 trace로 Jaeger에 표시)
```

**정산 (비동기 이벤트):**
```
Event/Odds: 경기 종료 → Kafka `EventSettled` 발행
  Betting 구독 → 승/패 판정 → 베팅 상태 갱신 → `BetSettled` 발행
    Wallet 구독 → 당첨금 Credit
    Ops 구독 → 통계 갱신
```

**실패/보상 정책(학습 단순화):** `PlaceBet`에서 Risk 승인 후 Wallet Debit 실패 시 베팅을 저장하지 않고 거절 응답. Debit 성공 후 저장 실패 같은 드문 경우는 보상 트랜잭션(Wallet Credit 롤백)으로 처리. 분산 트랜잭션(2PC)은 도입하지 않음.

## 7. 관측성 (목표 D)

- **메트릭:** 서비스별 Actuator + Micrometer → Prometheus 수집 → Grafana 대시보드(베팅 처리량, Risk 거절율, Wallet 실패율, gRPC 지연 분포).
- **분산추적:** OpenTelemetry 자동계측 → OTel Collector → Jaeger. gRPC 호출과 Kafka 발행/소비가 하나의 trace로 연결되는지 확인.
- **헬스체크:** Actuator health + gRPC health checking protocol.

## 8. 테스트 전략

- 서비스별 단위 테스트: 도메인 로직(배당 계산, 정산 판정, 한도 체크).
- 통합 테스트: Testcontainers로 Postgres·Kafka 기동, 실제 발행/구독 검증.
- 계약 테스트: gRPC in-process 서버로 서비스 간 호출 계약 검증.

## 9. 구현 순서

각 단계는 독립적으로 검증 가능한 목표를 가진다.

1. **인프라 스캐폴딩** — 모노레포 + docker-compose(인프라 전부) + proto 모듈.
   → 검증: `docker compose up` 정상 기동, 빈 서비스가 gRPC 헬스체크 응답.
2. **Event/Odds** (데이터 생산자).
   → 검증: `ListEvents`/`StreamOdds` 동작, `OddsChanged` 발행 확인.
3. **Wallet** (독립적).
   → 검증: Debit/Credit/잔액 정합, 원장 기록 확인.
4. **Betting** (오케스트레이터).
   → 검증: `PlaceBet` 전체 흐름 성공, Jaeger에 단일 trace 연결.
5. **BFF + 정적 화면**.
   → 검증: 브라우저에서 경기 목록 보기 / 베팅 걸기 / 잔액·내 베팅 조회 동작, Browser→BFF→백엔드가 하나의 trace로 연결.
6. **Risk**.
   → 검증: 한도 초과 베팅 거절, `BetPlaced` 기반 익스포저 집계.
7. **Ops/Admin + Grafana 대시보드**.
   → 검증: `GetSystemStatus` 조회, Grafana에서 핵심 메트릭 확인.

> 5단계까지 완료하면 "화면 → BFF → 동기 gRPC 오케스트레이션 + 분산추적"이라는 핵심 학습 경로가 성립한다.

## 10. 미해결/후속 항목

- BFF 화면 고도화(프레임워크 도입) — 정적 HTML/JS로 학습 후 필요 시 재검토.
- Kafka 정확히-한 번(exactly-once) 시맨틱 — 초기에는 at-least-once + 멱등 소비로 단순화.
- Loki 로그 집계, Alertmanager 알림 — 후속 확장.
