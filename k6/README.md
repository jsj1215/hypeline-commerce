# 부하 테스트

## 실행

```bash
# 인프라 + 모니터링 기동
docker compose -f ../docker/infra-compose.yml up -d
docker compose -f ../docker/monitoring-compose.yml up -d

# 스모크
k6 run -e PROFILE=smoke scenarios/01-show-browse.js

# 드롭 오픈 스파이크 (초과 판매 검증)
k6 run -e PROFILE=spike -e DROP_ID=1 scenarios/02-drop-purchase.js

# 결과를 Grafana 로 흘려보내기
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
  k6 run -o experimental-prometheus-rw -e PROFILE=load scenarios/01-show-browse.js
```

## 프로파일

| 이름 | 용도 |
|---|---|
| `smoke` | 시나리오가 깨지지 않는지 확인 (VU 5, 30초) |
| `load` | 평시 부하 재현 (VU 200, 5분) |
| `spike` | 드롭 오픈 순간 재현 (50 → 5,000 RPS 를 5초 만에) |
| `capacity` | 단일 인스턴스 처리량 한계 탐색 (임계 도달까지 계단식 증가) |

## 재고 차감 baseline

`05-stock-deduct.js` 는 재고 차감 경로의 처리량 한계를 잰다.
`SKU_SPREAD` 로 경합 정도를 바꿔가며 병목이 어디인지 구분한다.

```bash
# 사전 준비: 재고 시드 (품절이 나지 않도록 넉넉히)
docker exec hypeline-mysql mysql -uapplication -papplication hypeline -e \
  "INSERT INTO sku_stock (sku_id, quantity, version, created_at, updated_at)
   VALUES (1, 1000000, 0, NOW(6), NOW(6))"

# 모든 요청이 같은 SKU 를 노린다 (드롭 상황)
k6 run -e PROFILE=capacity -e SKU_ID=1 scenarios/05-stock-deduct.js

# SKU 100개로 흩는다 (경합 없음). 위와 비교해 병목이 단일 행인지 확인한다
k6 run -e PROFILE=capacity -e SKU_ID=1 -e SKU_SPREAD=100 scenarios/05-stock-deduct.js
```

측정 결과와 해석은 [docs/05-stock-concurrency.md](../docs/05-stock-concurrency.md) 참고.

## 측정 원칙

로컬 단일 머신에서는 부하 생성기 자체가 병목이 되므로 목표치인 50k RPS 를 그대로 재현하지 않는다.
대신 아래 순서로 근거를 만든다.

1. `capacity` 로 **단일 인스턴스**의 한계 RPS 와 그 지점의 P95 를 실측한다.
2. 인스턴스를 2대, 4대로 늘려 처리량이 선형으로 늘어나는지 확인한다.
3. 선형으로 늘지 않는 지점(단일 재고 키, 단일 DB row, Kafka 파티션 수)을 찾아 그 병목만 따로 측정한다.
4. 개선 전후를 같은 시나리오·같은 프로파일로 비교한다.

3번이 이 프로젝트의 핵심이다. 스케일아웃으로 풀리지 않는 병목이 어디인지가 실제 기술적 내용이다.
