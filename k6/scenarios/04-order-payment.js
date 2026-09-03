// 일반 주문·결제 경로. PG 지연·장애 시 서킷브레이커가 열려 응답이 유지되는지 본다.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE_URL, authHeaders, scenario } from '../lib/config.js';

const pgUnavailable = new Counter('pg_circuit_open');

export const options = {
  scenarios: scenario('order_payment'),
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    // 서킷이 열려도 요청 자체는 빠르게 실패해야 한다.
    'http_req_duration{expected_response:false}': ['p(95)<500'],
  },
};

export default function () {
  const memberId = __VU * 100000 + __ITER;
  const headers = { ...authHeaders(memberId), 'X-Idempotency-Key': `order-${memberId}` };

  const body = JSON.stringify({
    items: [{ productId: (__VU % 100) + 1, quantity: 1 }],
    paymentMethod: 'CARD',
  });

  const res = http.post(`${BASE_URL}/api/v1/orders`, body, { headers });
  if (res.json('meta.errorCode') === 'PAYMENT_GATEWAY_UNAVAILABLE') pgUnavailable.add(1);
  check(res, { 'order handled': (r) => r.status < 500 });
}
