# 도메인

## 경계

| 도메인 | 책임 | 핵심 불변식 |
|---|---|---|
| `member` | 회원 가입·조회 | 로그인 ID 는 유일하다 |
| `auth` | 인증 토큰 발급·검증 | — |
| `brand` | 판매 주체 | — |
| `product` | 상품 카탈로그 | — |
| `show` | 라이브 방송 편성과 상태 전이 | `SCHEDULED → ON_AIR → ENDED` 역방향 전이 불가 |
| `viewer` | 시청 세션, SSE 커넥션 | 커넥션은 수립한 인스턴스에만 존재한다 |
| `drop` | 방송 중 한정 특가 | 오픈 시각 이전·종료 이후 구매 불가 |
| `stock` | 재고 원장 | **재고는 절대 음수가 될 수 없다** |
| `waitroom` | 구매 대기열 | 입장 순서는 진입 순서를 따른다 |
| `cart` | 장바구니 | — |
| `order` | 주문 생성·상태 전이 | 같은 멱등키로는 주문이 하나만 생성된다 |
| `payment` | PG 결제와 콜백 | 결제 성공 없이 주문은 확정되지 않는다 |
| `coupon` | 쿠폰 발급·사용 | 발급 수량은 정해진 수를 넘지 않는다 |
| `point` | 적립금 원장 | 잔액은 음수가 될 수 없다 |
| `ranking` | 실시간 랭킹 조회 | — |
| `outbox` | 이벤트 적재·릴레이 | 도메인 커밋과 이벤트 적재는 같은 트랜잭션이다 |

## 상태 전이

### 방송

```
SCHEDULED ──▶ ON_AIR ──▶ ENDED
                 └──────▶ ABORTED   (사고로 중단)
```

### 드롭

```
WAITING ──(오픈 시각 도달)──▶ OPEN ──▶ SOLD_OUT
                                └────▶ CLOSED   (종료 시각 도달)
```

### 주문

```
CREATED ──▶ PAYMENT_PENDING ──▶ PAID ──▶ CONFIRMED
                  │                └────▶ REFUNDED
                  └──▶ PAYMENT_FAILED ──▶ CANCELED
```

## Redis 키 규약

| 용도 | 키 | 자료구조 |
|---|---|---|
| 드롭 재고 | `drop:{dropId}:stock:{shard}` | String (원자적 DECRBY) |
| 인당 구매 수량 | `drop:{dropId}:purchased:{memberId}` | String |
| 대기열 | `waitroom:{dropId}:queue` | Sorted Set (score = 진입 시각) |
| 입장 토큰 | `waitroom:{dropId}:token:{token}` | String (TTL 180s) |
| 멱등키 | `idem:{key}` | String (TTL 600s, SET NX) |
| 실시간 랭킹 | `ranking:show:{yyyyMMddHH}` | Sorted Set |
| SSE 팬아웃 | `show:{showId}:events` | Pub/Sub 채널 |

재고 키를 샤드로 쪼개는 이유는 단일 키가 Redis 단일 스레드·단일 샤드로 몰리기 때문이다.
`shard = memberId % shardCount` 로 분산하고, 소진 시 다른 샤드에서 빌려온다.

## Kafka 토픽

| 토픽 | 발행 | 소비 |
|---|---|---|
| `order.created` | commerce-api (Outbox Relay) | streamer (집계, 랭킹) |
| `drop.purchased` | commerce-api (Outbox Relay) | streamer (재고 팬아웃, 랭킹) |
| `show.viewer.changed` | commerce-api | streamer (동시 시청자 집계) |
| `payment.completed` | commerce-api (Outbox Relay) | streamer (정산 원장 적재) |

파티션 키는 `showId` 로 둔다. 같은 방송의 이벤트는 순서가 보장되어야 하기 때문이다.
