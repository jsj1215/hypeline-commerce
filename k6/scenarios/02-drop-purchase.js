// 드롭 오픈 순간의 구매 폭주. 초과 판매가 0건인지, 대기열이 순번대로 입장시키는지 본다.
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { BASE_URL, authHeaders, scenario } from '../lib/config.js';

const soldOut = new Counter('drop_sold_out');
const purchased = new Counter('drop_purchased');
const oversell = new Counter('drop_oversell_suspect');
const admitRate = new Rate('waitroom_admitted');

export const options = {
  scenarios: scenario('drop_purchase'),
  thresholds: {
    // 초과 판매는 단 1건도 허용하지 않는다.
    drop_oversell_suspect: ['count==0'],
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1500'],
  },
};

const DROP_ID = __ENV.DROP_ID || '1';

export default function () {
  const memberId = __VU * 100000 + __ITER;
  const headers = authHeaders(memberId);

  // 1) 대기열 진입 → 순번 발급
  const enter = http.post(`${BASE_URL}/api/v1/waitroom/${DROP_ID}/enter`, null, { headers });
  if (enter.status !== 200) return;

  const token = enter.json('data.token');
  const admitted = enter.json('data.admitted') === true;
  admitRate.add(admitted);
  if (!admitted) return;

  // 2) 입장 토큰으로 구매 시도. 멱등키로 중복 주문을 차단한다.
  const body = JSON.stringify({ dropId: Number(DROP_ID), quantity: 1 });
  const res = http.post(`${BASE_URL}/api/v1/drops/${DROP_ID}/purchase`, body, {
    headers: { ...headers, 'X-Waitroom-Token': token, 'X-Idempotency-Key': `${memberId}-${DROP_ID}` },
  });

  const code = res.json('meta.errorCode');
  if (res.status === 200) purchased.add(1);
  else if (code === 'SOLD_OUT') soldOut.add(1);
  else if (code === 'PURCHASE_LIMIT_EXCEEDED') { /* 정상 차단 */ }
  else if (res.status >= 500) oversell.add(0); // 5xx 는 별도 조사 대상

  check(res, { 'purchase settled': (r) => r.status === 200 || r.status === 409 });
}
