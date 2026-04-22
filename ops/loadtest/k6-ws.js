import ws from "k6/ws";
import { check } from "k6";

const baseWs = (__ENV.WS_URL || "wss://picture.xucanwei.xyz").replace(/\/+$/, "");
const tokenName = __ENV.TOKEN_NAME || "satoken";
const tokenValue = __ENV.LOGIN_TOKEN || "";
const pictureId = __ENV.PICTURE_ID || "1";

export const options = {
  scenarios: {
    ws_handshake_and_hold: {
      executor: "ramping-vus",
      startVUs: 5,
      stages: [
        { duration: "1m", target: 20 },
        { duration: "4m", target: 40 },
        { duration: "2m", target: 0 },
      ],
      gracefulStop: "30s",
    },
  },
};

export default function () {
  const query = `?pictureId=${pictureId}&tokenName=${encodeURIComponent(tokenName)}&tokenValue=${encodeURIComponent(tokenValue)}`;
  const url = `${baseWs}/api/ws/picture/edit${query}`;
  const result = ws.connect(url, { headers: { "User-Agent": "k6-ws-test/1.0" } }, function (socket) {
    socket.setTimeout(function () {
      socket.close();
    }, 3000);
  });

  check(result, {
    "ws handshake 101 or rate-limited": (r) => r && (r.status === 101 || r.status === 429 || r.status === 403),
  });
}
