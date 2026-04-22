import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "https://picture.xucanwei.xyz";

export const options = {
  scenarios: {
    baseline_read: {
      executor: "ramping-vus",
      startVUs: 20,
      stages: [
        { duration: "2m", target: 80 },
        { duration: "6m", target: 120 },
        { duration: "2m", target: 0 },
      ],
      gracefulRampDown: "30s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800"],
  },
};

export default function () {
  const home = http.get(`${baseUrl}/`);
  check(home, { "home 200": (r) => r.status === 200 });

  const tags = http.get(`${baseUrl}/api/picture/tag_category`);
  check(tags, { "tag api 200": (r) => r.status === 200 });

  sleep(1);
}
