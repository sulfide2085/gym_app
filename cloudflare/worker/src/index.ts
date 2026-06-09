export interface Env {
  DB: D1Database;
}

type AppStateRequest = {
  exercises?: unknown;
  workouts?: unknown;
};

type AuthRequest = {
  username?: string;
  password?: string;
};

type UserRow = {
  id: string;
  username: string;
  nickname: string | null;
  password_hash: string;
  password_salt: string;
  password_iterations: number;
};

type SessionRow = {
  user_id: string;
  username: string;
  nickname: string | null;
  expires_at: number;
};

type ProfileRequest = {
  nickname?: string;
};

const SESSION_DAYS = 30;
const PASSWORD_ITERATIONS = 60_000;
const LATEST_ANDROID_VERSION_CODE = 1;
const LATEST_ANDROID_VERSION_NAME = "1.0";
const ANDROID_APK_URL = "https://gym-app-releases.pages.dev/app-debug.apk";
const RELEASE_NOTES = ["账户同步", "我的页面", "软件更新检查"];

const jsonHeaders = {
  "content-type": "application/json; charset=utf-8",
  "access-control-allow-origin": "*",
  "access-control-allow-methods": "GET, PUT, POST, OPTIONS",
  "access-control-allow-headers": "content-type, authorization"
};

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: jsonHeaders });
    }

    if (url.pathname === "/health") {
      return json({ ok: true });
    }

    if (url.pathname === "/api/app/update" && request.method === "GET") {
      const currentVersionCode = Number(url.searchParams.get("versionCode") ?? "0");
      return json({
        platform: "android",
        latestVersionCode: LATEST_ANDROID_VERSION_CODE,
        latestVersionName: LATEST_ANDROID_VERSION_NAME,
        hasUpdate: currentVersionCode < LATEST_ANDROID_VERSION_CODE,
        required: false,
        apkUrl: ANDROID_APK_URL,
        releaseNotes: RELEASE_NOTES
      });
    }

    if (url.pathname === "/api/auth/register" && request.method === "POST") {
      return register(request, env);
    }

    if (url.pathname === "/api/auth/login" && request.method === "POST") {
      return login(request, env);
    }

    if (url.pathname === "/api/auth/me" && request.method === "GET") {
      const session = await requireSession(request, env);
      if (session instanceof Response) return session;
      return json({
        user: {
          id: session.user_id,
          username: session.username,
          nickname: session.nickname ?? session.username
        },
        expiresAt: session.expires_at
      });
    }

    if (url.pathname === "/api/auth/me" && request.method === "PATCH") {
      const session = await requireSession(request, env);
      if (session instanceof Response) return session;
      return updateProfile(request, env, session);
    }

    if (url.pathname === "/api/auth/logout" && request.method === "POST") {
      const tokenHash = await bearerTokenHash(request);
      if (tokenHash) {
        await env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(tokenHash).run();
      }
      return json({ ok: true });
    }

    if (url.pathname !== "/api/state") {
      return json({ error: "Not found" }, 404);
    }

    const session = await requireSession(request, env);
    if (session instanceof Response) return session;

    if (request.method === "GET") {
      return getState(env, session.user_id);
    }

    if (request.method === "PUT") {
      return saveState(request, env, session.user_id);
    }

    return json({ error: "Method not allowed" }, 405);
  }
};

async function register(request: Request, env: Env): Promise<Response> {
  const credentials = await parseCredentials(request);
  if (credentials instanceof Response) return credentials;

  const existing = await env.DB.prepare("SELECT id FROM users WHERE username = ?")
    .bind(credentials.username)
    .first<{ id: string }>();
  if (existing) {
    return json({ error: "用户名已存在" }, 409);
  }

  const salt = randomBytes(16);
  const passwordHash = await hashPassword(credentials.password, salt, PASSWORD_ITERATIONS);
  const userId = crypto.randomUUID();
  const now = Date.now();

  await env.DB.prepare(
    `INSERT INTO users (id, username, password_hash, password_salt, password_iterations, created_at)
     VALUES (?, ?, ?, ?, ?, ?)`
  ).bind(
    userId,
    credentials.username,
    passwordHash,
    base64UrlEncode(salt),
    PASSWORD_ITERATIONS,
    now
  ).run();

  return createSession(env, userId, credentials.username);
}

async function login(request: Request, env: Env): Promise<Response> {
  const credentials = await parseCredentials(request);
  if (credentials instanceof Response) return credentials;

  const user = await env.DB.prepare(
    "SELECT id, username, password_hash, password_salt, password_iterations FROM users WHERE username = ?"
  ).bind(credentials.username).first<UserRow>();

  if (!user) {
    return json({ error: "用户名或密码错误" }, 401);
  }

  const candidateHash = await hashPassword(
    credentials.password,
    base64UrlDecode(user.password_salt),
    user.password_iterations
  );

  if (!constantTimeEqual(candidateHash, user.password_hash)) {
    return json({ error: "用户名或密码错误" }, 401);
  }

  return createSession(env, user.id, user.username);
}

async function createSession(env: Env, userId: string, username: string): Promise<Response> {
  const token = base64UrlEncode(randomBytes(32));
  const tokenHash = await sha256Base64Url(token);
  const now = Date.now();
  const expiresAt = now + SESSION_DAYS * 24 * 60 * 60 * 1000;

  await env.DB.prepare(
    "INSERT INTO sessions (token_hash, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)"
  ).bind(tokenHash, userId, now, expiresAt).run();

  return json({
    token,
    user: {
      id: userId,
      username,
      nickname: username
    },
    expiresAt
  });
}

async function requireSession(request: Request, env: Env): Promise<SessionRow | Response> {
  const tokenHash = await bearerTokenHash(request);
  if (!tokenHash) {
    return json({ error: "Unauthorized" }, 401);
  }

  const session = await env.DB.prepare(
    `SELECT sessions.user_id, users.username, users.nickname, sessions.expires_at
     FROM sessions
     JOIN users ON users.id = sessions.user_id
     WHERE sessions.token_hash = ?`
  ).bind(tokenHash).first<SessionRow>();

  if (!session || session.expires_at <= Date.now()) {
    await env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(tokenHash).run();
    return json({ error: "Unauthorized" }, 401);
  }

  return session;
}

async function updateProfile(request: Request, env: Env, session: SessionRow): Promise<Response> {
  let body: ProfileRequest;
  try {
    body = await request.json<ProfileRequest>();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const nickname = normalizeNickname(body.nickname, session.username);

  await env.DB.prepare("UPDATE users SET nickname = ? WHERE id = ?")
    .bind(nickname, session.user_id)
    .run();

  return json({
    user: {
      id: session.user_id,
      username: session.username,
      nickname
    }
  });
}

async function bearerTokenHash(request: Request): Promise<string | null> {
  const header = request.headers.get("authorization") ?? "";
  const match = header.match(/^Bearer\s+(.+)$/i);
  if (!match) return null;
  return sha256Base64Url(match[1]);
}

async function getState(env: Env, userId: string): Promise<Response> {
  const row = await env.DB.prepare(
    "SELECT user_id, exercises_json, workouts_json, updated_at FROM app_state WHERE user_id = ?"
  ).bind(userId).first<{
    user_id: string;
    exercises_json: string;
    workouts_json: string;
    updated_at: number;
  }>();

  if (!row) {
    return json({
      userId,
      exercises: [],
      workouts: [],
      updatedAt: 0
    });
  }

  return json({
    userId: row.user_id,
    exercises: JSON.parse(row.exercises_json),
    workouts: JSON.parse(row.workouts_json),
    updatedAt: row.updated_at
  });
}

async function saveState(request: Request, env: Env, userId: string): Promise<Response> {
  let body: AppStateRequest;
  try {
    body = await request.json<AppStateRequest>();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const exercises = Array.isArray(body.exercises) ? body.exercises : [];
  const workouts = Array.isArray(body.workouts) ? body.workouts : [];
  const updatedAt = Date.now();

  await env.DB.prepare(
    `INSERT INTO app_state (user_id, exercises_json, workouts_json, updated_at)
     VALUES (?, ?, ?, ?)
     ON CONFLICT(user_id) DO UPDATE SET
       exercises_json = excluded.exercises_json,
       workouts_json = excluded.workouts_json,
       updated_at = excluded.updated_at`
  ).bind(
    userId,
    JSON.stringify(exercises),
    JSON.stringify(workouts),
    updatedAt
  ).run();

  return json({ ok: true, userId, updatedAt });
}

async function parseCredentials(request: Request): Promise<{ username: string; password: string } | Response> {
  let body: AuthRequest;
  try {
    body = await request.json<AuthRequest>();
  } catch {
    return json({ error: "Invalid JSON" }, 400);
  }

  const username = normalizeUsername(body.username);
  const password = body.password ?? "";

  if (!username) {
    return json({ error: "请输入用户名" }, 400);
  }

  if (password.length < 8 || password.length > 128) {
    return json({ error: "密码至少 8 位" }, 400);
  }

  return { username, password };
}

function normalizeUsername(value: string | undefined): string {
  const trimmed = value?.trim().toLowerCase() ?? "";
  if (trimmed.includes("codex")) return "";
  if (!/^[a-z0-9_@.-]{3,40}$/.test(trimmed)) return "";
  return trimmed;
}

function normalizeNickname(value: string | undefined, fallback: string): string {
  const trimmed = value?.trim() ?? "";
  if (!trimmed) return fallback;
  return trimmed.slice(0, 24);
}

async function hashPassword(password: string, salt: Uint8Array, iterations: number): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(password),
    "PBKDF2",
    false,
    ["deriveBits"]
  );
  const bits = await crypto.subtle.deriveBits(
    {
      name: "PBKDF2",
      hash: "SHA-256",
      salt,
      iterations
    },
    key,
    256
  );
  return base64UrlEncode(new Uint8Array(bits));
}

async function sha256Base64Url(value: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return base64UrlEncode(new Uint8Array(digest));
}

function constantTimeEqual(a: string, b: string): boolean {
  const encoder = new TextEncoder();
  const left = encoder.encode(a);
  const right = encoder.encode(b);
  const length = Math.max(left.length, right.length);
  let diff = left.length ^ right.length;

  for (let i = 0; i < length; i += 1) {
    diff |= (left[i] ?? 0) ^ (right[i] ?? 0);
  }

  return diff === 0;
}

function randomBytes(size: number): Uint8Array {
  const bytes = new Uint8Array(size);
  crypto.getRandomValues(bytes);
  return bytes;
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function base64UrlDecode(value: string): Uint8Array {
  const padded = value.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: jsonHeaders
  });
}
