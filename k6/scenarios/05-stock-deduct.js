// 재고 차감 baseline. DB 조건부 UPDATE 만으로 어디까지 버티는지 실측한다.
// 이 수치가 Redis 도입 PR 의 비교 기준이 된다.
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { BASE_URL, authHeaders, scenario } from '../lib/config.js';

const sold = new Counter('stock_sold');
const soldOut = new Counter('stock_sold_out');
const oversellSuspect = new Counter('stock_oversell_suspect');
const deductDuration = new Trend('stock_deduct_duration');
const successRate = new Rate('stock_deduct_success');

export const options = {
  scenarios: scenario('stock_deduct'),
  thresholds: {
    // 초과 판매는 단 1건도 허용하지 않는다.
    stock_oversell_suspect: ['count==0'],
    // 재고 부족(409)은 정상 응답이므로 실패로 세지 않는다. 5xx 만 실패다.
    http_req_failed: ['rate<0.01'],
    stock_deduct_duration: ['p(95)<1000', 'p(99)<3000'],
  },
};

const SKU_ID = Number(__ENV.SKU_ID || 1);
// 몇 개의 SKU 로 부하를 흩을지. 1 이면 모든 요청이 같은 행을 노린다(드롭 상황).
// 2 이상이면 서로 다른 행으로 흩어져 단일 행 경합이 사라진다.
// 두 경우를 비교하면 병목이 단일 행 락인지 시스템 전반인지 구분된다.
const SKU_SPREAD = Number(__ENV.SKU_SPREAD || 1);

export default function () {
  const memberId = __VU * 100000 + __ITER;
  const skuId = SKU_ID + (SKU_SPREAD > 1 ? __VU % SKU_SPREAD : 0);
  const body = JSON.stringify({ quantity: 1 });

  const res = http.post(`${BASE_URL}/api/v1/skus/${skuId}/stock/deduct`, body, {
    headers: authHeaders(memberId),
  });

  deductDuration.add(res.timings.duration);

  if (res.status === 200) {
    sold.add(1);
    successRate.add(true);
  } else if (res.json('meta.errorCode') === 'SOLD_OUT') {
    soldOut.add(1);
    successRate.add(false);
  } else if (res.status >= 500) {
    // 5xx 는 재고 정합성을 알 수 없는 상태다. 조사 대상으로 센다.
    oversellSuspect.add(1);
    successRate.add(false);
  }

  check(res, { 'deduct settled': (r) => r.status === 200 || r.status === 409 });
}
