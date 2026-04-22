import http from "k6/http";
import { check, sleep } from "k6";

const baseUrl = __ENV.BASE_URL || "https://picture.xucanwei.xyz";

export const options = {
  scenarios: {
    abusive_single_ip: {
      executor: "constant-vus",
      vus: 120,
      duration: "2m",
    },
  },
};

export default function () {
  const payload = JSON.stringify({ userAccount: "x", userPassword: "y" });
  const res = http.post(`${baseUrl}/api/user/login`, payload, {
    headers: {
      "Content-Type": "application/json",
      "User-Agent": "sqlmap/1.7#stable",
    },
  });
  check(res, {
    "blocked by anti crawler or rate-limit": (r) => [403, 429].includes(r.status),
  });
  sleep(0.2);
}
