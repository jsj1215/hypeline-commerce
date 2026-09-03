// 라이브 방송 SSE 팬아웃. 인스턴스 N대에 흩어진 커넥션에 이벤트가 모두 도달하는지 본다.
// 주의: k6 는 SSE 를 스트리밍으로 소비하지 못하므로 연결 수립 지연과 첫 이벤트 도달 시간만 측정한다.
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, authHeaders } from '../lib/config.js';

const connectDuration = new Trend('sse_connect_duration');

export const options = {
  scenarios: {
    sse_fanout: {
      executor: 'ramping-vus',
      stages: [
        { duration: '1m', target: Number(__ENV.CONNECTIONS || 2000) },
        { duration: '3m', target: Number(__ENV.CONNECTIONS || 2000) },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    sse_connect_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};

const SHOW_ID = __ENV.SHOW_ID || '1';

export default function () {
  const memberId = __VU * 100000 + __ITER;
  const res = http.get(`${BASE_URL}/api/v1/shows/${SHOW_ID}/stream`, {
    headers: { ...authHeaders(memberId), Accept: 'text/event-stream' },
    timeout: '30s',
  });
  connectDuration.add(res.timings.duration);
  check(res, { 'sse connected': (r) => r.status === 200 });
}
