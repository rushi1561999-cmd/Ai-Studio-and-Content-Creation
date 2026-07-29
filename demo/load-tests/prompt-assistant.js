import http from "k6/http";
import { check, sleep } from "k6";
import exec from "k6/execution";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081/api";
const users = JSON.parse(__ENV.K6_USERS || "[]");
const sessions = {};

export const options = {
  stages: [
    { duration: "1m", target: 10 },
    { duration: "3m", target: 25 },
    { duration: "3m", target: 50 },
    { duration: "1m", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<5500"],
  },
};

function sessionForVu() {
  const vu = exec.vu.idInTest;
  if (sessions[vu]) return sessions[vu];
  const user = users[(vu - 1) % users.length];
  if (!user) {
    throw new Error("K6_USERS must contain at least one dedicated test account.");
  }

  const login = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: user.email, password: user.password }),
    { headers: { "Content-Type": "application/json" } },
  );
  const token = login.json("token");
  const workspaceResponse = http.get(`${BASE_URL}/workspaces`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  sessions[vu] = {
    token,
    workspaceId: user.workspaceId || workspaceResponse.json("0.id"),
  };
  return sessions[vu];
}

export default function () {
  const session = sessionForVu();
  const response = http.post(
    `${BASE_URL}/prompt-assistant/suggestions`,
    JSON.stringify({
      workspaceId: session.workspaceId,
      contentType: "TEXT",
      variantCount: 3,
      promptText:
        "Create a professional product launch email for a privacy-focused AI meeting assistant.",
    }),
    {
      headers: {
        Authorization: `Bearer ${session.token}`,
        "Content-Type": "application/json",
      },
    },
  );

  check(response, {
    "suggestion succeeds": (result) => result.status === 200,
    "suggestion has optimized prompt": (result) =>
      Boolean(result.json("optimizedPrompt")),
  });

  // 8.5 requests/minute per VU: below the production limit of 10.
  sleep(7);
}
