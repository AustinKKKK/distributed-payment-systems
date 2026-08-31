# 분산 결제 처리 시스템

[한국어](./README.md) | [English](./README.en.md)

단일 서버 동기 처리에서 시작해, 실제로 처리량이 무너지는 지점을 찾고 Kafka·Redis·Primary/Replica 분리까지 단계적으로 발전시킨 프로젝트입니다. 각 단계는 "이 정도 볼륨을 처리한다"는 목표가 아니라, **벽에 부딪히고 → 측정하고 → 원인을 이해하고 → 고친다**는 원칙으로 진행했습니다.

## 전체 아키텍처

![Full System Architecture](./full-architecture-tier1-4.svg)

## 단계별 요약

| 단계 | 문제 | 시도 | 결과 |
|---|---|---|---|
| Tier 1 | 커넥션 풀 고갈로 처리량 한계 | HikariCP 풀 크기 튜닝 (10→50) | 처리량 2.4배 (3.6K → 8.5K req/s) |
| Tier 2 | 외부 API 호출 지연으로 처리량 붕괴, 실패율 3.75~6.35% | Kafka로 결제 접수·처리 분리 | 접수 처리량 167배 (24.5 → 4,082 req/s), 실패율 0% |
| Tier 3 | 잔액 조회마다 원장 전체를 재계산 | Redis 캐싱 (evict-on-write) | 처리량 38.4배 (776 → 29,787 req/s) |
| Tier 4 | 단일 DB가 읽기·쓰기를 동시에 부담 | Primary/Replica 분리 + 복제 지연 재현 | 로컬 환경 물리 자원 공유로 성능 이득 없음(가설 반박, 원인 규명); read-your-own-writes 버그 재현·해결 |

**Tier 4는 "성공한 최적화"가 아니라 "예측이 틀린 것을 실측으로 확인하고, 그 과정에서 실제 정합성 버그를 겪고 고친" 사례입니다.** 로컬 단일 머신에서 Primary/Replica를 분리해도 두 DB가 같은 CPU·디스크를 공유하기 때문에 처리량이 오히려 소폭 하락했고, 이 결과를 숨기지 않고 원인을 함께 기록했습니다.

## 각 단계 상세

- [Tier 1 — 단일 서버, 동기 처리](./tier1)
- [Tier 2 — Kafka를 통한 비동기 처리](./tier2)
- [Tier 3 — Redis 캐싱](./tier3)
- [Tier 4 — Primary/Replica 분리와 복제 지연](./tier4)

## 스택

Java 21 · Spring Boot · PostgreSQL · Redis · Apache Kafka · Docker Compose · k6 (부하 테스트)

## 핵심 설계 원칙

- 결제 원장은 append-only, 복식부기(double-entry) — 잔액은 저장된 컬럼이 아니라 원장에서 항상 유도
- 멱등성은 DB UNIQUE 제약으로 물리적으로 보장 (애플리케이션 체크가 아님)
- 각 단계에서 도구(Kafka, Redis, Replica)는 측정된 병목이 있을 때만 도입
- 예측을 먼저 세우고 부하 테스트로 검증 — 예측이 틀렸을 때도 그대로 기록
