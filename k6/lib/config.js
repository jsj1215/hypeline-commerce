// 부하 테스트 공통 설정. 환경변수로 대상과 강도를 바꾼다.
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const THRESHOLDS = {
  http_req_failed: ['rate<0.01'],                 // 실패율 1% 미만
  http_req_duration: ['p(95)<500', 'p(99)<1000'], // P95 500ms / P99 1s
};

export const HEADERS = {
  'Content-Type': 'application/json',
};

export function authHeaders(memberId) {
  return { ...HEADERS, 'X-Member-Id': String(memberId) };
}

// 부하 프로파일. k6 run -e PROFILE=spike 로 선택한다.
export const PROFILES = {
  smoke: { executor: 'constant-vus', vus: 5, duration: '30s' },
  load: {
    executor: 'ramping-vus',
    stages: [
      { duration: '1m', target: 200 },
      { duration: '3m', target: 200 },
      { duration: '1m', target: 0 },
    ],
  },
  // 드롭 오픈 순간을 재현한다. 예열 없이 순간적으로 밀어넣는다.
  spike: {
    executor: 'ramping-arrival-rate',
    startRate: 50,
    timeUnit: '1s',
    preAllocatedVUs: 500,
    maxVUs: 3000,
    stages: [
      { duration: '10s', target: 50 },
      { duration: '5s', target: 5000 },
      { duration: '30s', target: 5000 },
      { duration: '10s', target: 50 },
    ],
  },
  // 단일 인스턴스 처리량 한계를 찾는다. 실패 임계에 닿을 때까지 올린다.
  capacity: {
    executor: 'ramping-arrival-rate',
    startRate: 100,
    timeUnit: '1s',
    preAllocatedVUs: 500,
    maxVUs: 5000,
    stages: [
      { duration: '30s', target: 500 },
      { duration: '30s', target: 1000 },
      { duration: '30s', target: 2000 },
      { duration: '30s', target: 4000 },
      { duration: '30s', target: 8000 },
    ],
  },
};

export function scenario(name) {
  const profile = __ENV.PROFILE || 'load';
  return { [name]: { ...PROFILES[profile], tags: { scenario: name } } };
}
