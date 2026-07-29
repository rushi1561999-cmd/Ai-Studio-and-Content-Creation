import http from "k6/http";
import { check, group, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081/api";

export const options = {
  stages: [
    { duration: "2m", target: 25 },
    { duration: "3m", target: 50 },
    { duration: "5m", target: 100 },
    { duration: "5m", target: 250 },
    { duration: "5m", target: 500 },
    { duration: "2m", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500"],
  },
};

export function setup() {
  if (!__ENV.TEST_EMAIL || !__ENV.TEST_PASSWORD) {
    throw new Error("Set TEST_EMAIL and TEST_PASSWORD for a dedicated load-test user.");
  }

  const login = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({
      email: __ENV.TEST_EMAIL,
      password: __ENV.TEST_PASSWORD,
    }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(login, { "login succeeds": (response) => response.status === 200 });
  const token = login.json("token");

  const workspaces = http.get(`${BASE_URL}/workspaces`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const workspaceId = __ENV.WORKSPACE_ID || workspaces.json("0.id");
  if (!token || !workspaceId) {
    throw new Error("Load-test login did not return a token/workspace.");
  }
  return { token, workspaceId };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };

  group("workspace dashboard", () => {
    const responses = http.batch([
      ["GET", `${BASE_URL}/ai/wallet/${data.workspaceId}`, null, { headers }],
      [
        "GET",
        `${BASE_URL}/ai/jobs/workspace/${data.workspaceId}?page=0&size=20`,
        null,
        { headers },
      ],
      [
        "GET",
        `${BASE_URL}/ai/contents/workspace/${data.workspaceId}?page=0&size=20`,
        null,
        { headers },
      ],
      [
        "GET",
        `${BASE_URL}/prompts/workspace/${data.workspaceId}?page=0&size=20`,
        null,
        { headers },
      ],
    ]);
    check(responses, {
      "dashboard requests succeed": (items) =>
        items.every((response) => response.status === 200),
    });
  });

  sleep(1);
}
