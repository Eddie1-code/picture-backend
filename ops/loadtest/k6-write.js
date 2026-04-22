import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "https://picture.xucanwei.xyz";
const token = __ENV.LOGIN_TOKEN || "";

export const options = {
  scenarios: {
    write_peak: {
      executor: "ramping-vus",
      startVUs: 10,
      stages: [
        { duration: "2m", target: 50 },
        { duration: "5m", target: 100 },
        { duration: "3m", target: 150 },
        { duration: "2m", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<1200"],
  },
};

function authHeaders() {
  const headers = {
    "Content-Type": "application/json",
    "User-Agent": "k6-write-test/1.0",
  };
  if (token) {
    headers.satoken = token;
  }
  return headers;
}

export default function () {
  const payload = JSON.stringify({
    current: 1,
    pageSize: 10,
  });
  const res = http.post(`${baseUrl}/api/post/list/page/vo`, payload, {
    headers: authHeaders(),
  });
  check(res, {
    "write scenario reachable": (r) => r.status === 200 || r.status === 401 || r.status === 429,
  });
  sleep(1);
}
