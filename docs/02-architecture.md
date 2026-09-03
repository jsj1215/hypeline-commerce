# 아키텍처

## 모듈 구성

```
hypeline-commerce
├── apps                    실행 가능한 SpringBootApplication
│   ├── commerce-api        HTTP API. 10~20대로 스케일아웃한다
│   ├── commerce-streamer   Kafka 컨슈머. 집계·랭킹·SSE 팬아웃 트리거
│   └── commerce-batch      Spring Batch. 정산·스냅샷·정합성 대사
├── modules                 도메인에 의존하지 않는 재사용 설정
│   ├── jpa                 DataSource, JPA, QueryDSL
│   ├── redis               Lettuce Master/Replica, RedisTemplate
│   └── kafka               Producer/Consumer, 배치 리스너 팩토리
└── supports                부가 기능 add-on
    ├── jackson             ObjectMapper 정책
    ├── logging             Logback, Slack appender
    └── monitoring          Actuator, Micrometer Prometheus
```

`apps` 는 서로를 참조하지 않는다. 공유가 필요한 것은 Kafka 메시지 스키마뿐이고, 이는 각 앱이 자기 DTO 로 갖는다.

## 레이어 규칙

```
interfaces ──▶ application ──▶ domain ◀── infrastructure
```

| 레이어 | 책임 | 금지 |
|---|---|---|
| `interfaces` | HTTP/Kafka 진입점, DTO 변환, 인증 | 비즈니스 판단 |
| `application` | 유스케이스 조립, 트랜잭션 경계 | 비즈니스 규칙 자체 |
| `domain` | 엔티티, 값 객체, 도메인 서비스, **저장소 인터페이스** | 스프링/JPA 외 프레임워크 의존 |
| `infrastructure` | 저장소 구현, 외부 API 클라이언트, Redis/Kafka 어댑터 | 도메인 규칙 |

`domain` 이 저장소 인터페이스를 선언하고 `infrastructure` 가 구현한다. 의존 방향은 항상 `domain` 을 향한다.

## 애플리케이션 포트

| 앱 | 서비스 | 관리(Actuator) |
|---|---|---|
| commerce-api | 8080 | 8081 |
| commerce-streamer | 8090 | 8091 |
| commerce-batch | — | 8093 |

다중 인스턴스로 띄울 때는 `--server.port` 와 `--management.server.port` 를 함께 올린다.

```bash
java -jar commerce-api.jar --server.port=8080 --management.server.port=8081
java -jar commerce-api.jar --server.port=8180 --management.server.port=8181
```

띄운 관리 포트를 `docker/grafana/prometheus.yml` 의 target 에 추가해야 지표가 수집된다.

## 인프라

| 컴포넌트 | 용도 |
|---|---|
| MySQL 8.0 | 주문·상품·회원 원장. 조회는 리플리카로 분리 예정 |
| Redis 7.2 (master/replica) | 재고 원자적 차감, 대기열 ZSET, 멱등키, 랭킹 ZSET, Pub/Sub |
| Kafka 3.7 (KRaft, 12 파티션) | 도메인 이벤트 전달 |
| Prometheus + Grafana | 지표 수집과 대시보드. k6 결과도 remote write 로 함께 본다 |

Redis 는 `maxmemory-policy` 를 `noeviction` 으로 둔다. 재고와 대기열 키가 evict 되면 초과 판매가 발생한다.
