// 방송 목록·상세 조회. 읽기 트래픽 대부분을 차지하는 경로로, 캐시 적중률과 함께 본다.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, HEADERS, THRESHOLDS, scenario } from '../lib/config.js';

const showDetailDuration = new Trend('show_detail_duration');

export const options = {
  scenarios: scenario('show_browse'),
  thresholds: THRESHOLDS,
};

export default function () {
  const list = http.get(`${BASE_URL}/api/v1/shows?status=ON_AIR&page=0&size=20`, { headers: HEADERS });
  check(list, { 'show list 200': (r) => r.status === 200 });

  const showId = (__VU % 50) + 1;
  const detail = http.get(`${BASE_URL}/api/v1/shows/${showId}`, { headers: HEADERS });
  showDetailDuration.add(detail.timings.duration);
  check(detail, { 'show detail 200': (r) => r.status === 200 });

  sleep(1);
}
