# Hypeline

브랜드가 라이브 방송을 켜고 방송 도중 한정 수량 의류를 파는(**드롭**) 커머스 백엔드.

인기 방송 하나에 시청자 수만 명이 붙어 사이즈별 잔여 수량을 실시간으로 지켜보다가,
드롭이 열리는 순간 동시에 구매를 시도한다. 읽기 팬아웃과 쓰기 폭주가 같은 시점에 오는 것이
이 서비스의 부하 특성이다.

의류라서 재고가 상품이 아니라 **SKU(상품 × 사이즈 × 컬러) 단위**로 쪼개진다.
같은 후드티라도 M 블랙만 먼저 소진되고, 경합은 그 하나의 SKU 로 집중된다.

## 목표 규모

| 항목 | 값 |
|---|---|
| DAU | 500만 |
| 평시 / 피크 처리량 | 3,000 RPS / 50,000 RPS |
| 동시 SSE 커넥션 | 300,000 |
| API 인스턴스 | 10~20대 |

자세한 내용과 검증 방법은 [docs/01-concept.md](docs/01-concept.md) 참고.

## 기술 스택

`Java 21` `Spring Boot 3.4` `MySQL 8` `Redis 7.2` `Kafka 3.7 (KRaft)` `Spring Batch` `QueryDSL`
`Resilience4j` `Prometheus` `Grafana` `k6` `Testcontainers`

## 시작하기

### 1. 인프라 기동

```bash
docker compose -f docker/infra-compose.yml up -d
```

MySQL(3306), Redis master(6379)/replica(6380), Kafka(19092), Kafka UI(9099) 가 뜬다.

### 2. 모니터링 기동

```bash
docker compose -f docker/monitoring-compose.yml up -d
```

Grafana 는 http://localhost:3000 (admin / admin), Prometheus 는 http://localhost:9090.

### 3. 애플리케이션 실행

```bash
./gradlew :apps:commerce-api:bootRun
./gradlew :apps:commerce-streamer:bootRun

# 배치는 실행할 Job 을 지정한다
./gradlew :apps:commerce-batch:bootRun --args='--spring.batch.job.name=settlementJob'
```

API 는 http://localhost:8080, Swagger 는 http://localhost:8080/swagger-ui.html.

### 4. 부하 테스트

```bash
cd k6
k6 run -e PROFILE=smoke scenarios/01-show-browse.js
k6 run -e PROFILE=spike -e DROP_ID=1 scenarios/02-drop-purchase.js
```

프로파일과 측정 원칙은 [k6/README.md](k6/README.md) 참고.

## 프로젝트 구조

```
├── apps                    실행 가능한 SpringBootApplication
│   ├── commerce-api        HTTP API. 스케일아웃 대상
│   ├── commerce-streamer   Kafka 컨슈머. 집계·랭킹·SSE 팬아웃
│   └── commerce-batch      Spring Batch. 정산·스냅샷·정합성 대사
├── modules                 도메인 무관 재사용 설정 (jpa, redis, kafka)
├── supports                add-on (jackson, logging, monitoring)
├── docker                  로컬 인프라와 모니터링 compose
├── k6                      부하 테스트 시나리오
└── docs                    설계 문서
```

레이어 규칙과 의존 방향은 [docs/02-architecture.md](docs/02-architecture.md),
도메인 경계와 Redis 키·Kafka 토픽 규약은 [docs/03-domain.md](docs/03-domain.md) 참고.

## 요구 환경

- JDK 21 (없으면 Gradle toolchain 이 자동으로 받아온다)
- Docker / Docker Compose
- k6 (부하 테스트 시)

## 라이선스

Apache License 2.0
