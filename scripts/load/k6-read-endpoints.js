// 공개 읽기 경로 부하 시나리오.
// 목적은 최대 처리량 자랑이 아니라, 어느 지점에서 응답이 무너지는지 찾는 것이다.
//
// 실행 (k6 설치 없이 Docker 로):
//   docker run --rm -i --add-host=host.docker.internal:host-gateway \
//     -e BASE_URL=http://host.docker.internal:9090 grafana/k6 run - < scripts/load/k6-read-endpoints.js
//
// 대상 서버는 H2 프로파일로 띄운다:
//   ./gradlew bootRun --args='--spring.profiles.active=h2'

import http from 'k6/http';
import { check, group } from 'k6';
import { Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:9090';

// 엔드포인트별로 따로 본다. 합산 p95는 무거운 경로를 가벼운 경로가 가려버린다.
const tFacilities = new Trend('t_facilities', true);
const tSafetyScore = new Trend('t_safety_score', true);
const tBoard = new Trend('t_board', true);
const tMapPage = new Trend('t_map_page', true);

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 1,
      // 1 에서 60 VU 까지 올리며 꺾이는 지점을 찾는다
      stages: [
        { duration: '20s', target: 10 },
        { duration: '30s', target: 30 },
        { duration: '30s', target: 60 },
        { duration: '20s', target: 60 },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    // 읽기 전용 경로다. 실패가 1%를 넘으면 그 시점 부하는 감당 못 하는 것으로 본다
    http_req_failed: ['rate<0.01'],
    // 시설 조회는 응답이 380KB 대라 별도 기준을 둔다
    t_facilities: ['p(95)<1500'],
    t_safety_score: ['p(95)<500'],
    t_board: ['p(95)<300'],
    t_map_page: ['p(95)<800'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

// 서울 전역 bbox. 실제 지도 화면이 보내는 형태와 같다
const BBOX = 'swLat=37.4&swLng=126.8&neLat=37.7&neLng=127.2';

// k6 는 Accept-Encoding 을 자동으로 붙이지 않는다. 브라우저는 붙인다.
// 이걸 빼고 재면 서버 압축이 켜져 있어도 압축된 응답을 한 번도 받지 않아,
// 실제 사용자가 겪는 전송량과 다른 값을 재게 된다.
const HEADERS = { 'Accept-Encoding': 'gzip' };

export default function () {
  group('facilities-bbox', () => {
    const r = http.get(`${BASE}/api/facilities?type=shelter&${BBOX}`, { headers: HEADERS, tags: { ep: 'facilities' } });
    tFacilities.add(r.timings.duration);
    check(r, { 'facilities 200': (x) => x.status === 200 });
  });

  group('safety-score', () => {
    const r = http.get(`${BASE}/api/safety-score?lat=37.5665&lng=126.9780`, { headers: HEADERS, tags: { ep: 'safety_score' } });
    tSafetyScore.add(r.timings.duration);
    check(r, { 'safety-score 200': (x) => x.status === 200 });
  });

  group('board-list', () => {
    const r = http.get(`${BASE}/api/board`, { headers: HEADERS, tags: { ep: 'board' } });
    tBoard.add(r.timings.duration);
    check(r, { 'board 200': (x) => x.status === 200 });
  });

  group('map-page', () => {
    const r = http.get(`${BASE}/map`, { headers: HEADERS, tags: { ep: 'map_page' } });
    tMapPage.add(r.timings.duration);
    check(r, { 'map 200': (x) => x.status === 200 });
  });
}
