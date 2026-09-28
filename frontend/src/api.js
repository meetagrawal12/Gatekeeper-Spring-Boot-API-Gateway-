// Single place that talks to the management service. Adds the bearer token,
// normalises errors, and signals the app to sign out on 401.
const BASE = import.meta.env.VITE_API_BASE ?? "/api";
const KEY = "gk_token";

export const tokenStore = {
  get: () => sessionStorage.getItem(KEY),
  set: (t) => sessionStorage.setItem(KEY, t),
  clear: () => sessionStorage.removeItem(KEY),
};

let unauthorizedHandler = () => {};
export const onUnauthorized = (fn) => {
  unauthorizedHandler = fn;
};

export class ApiError extends Error {
  constructor(status, message, details = []) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

export async function api(path, { method = "GET", body, params, auth = true } = {}) {
  const url = new URL(BASE + path, window.location.origin);
  Object.entries(params ?? {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") url.searchParams.set(k, v);
  });

  const headers = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const token = tokenStore.get();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(url, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  } catch {
    throw new ApiError(0, "Cannot reach the server. Is the backend running?");
  }

  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);

  if (!res.ok) {
    if (res.status === 401 && auth) unauthorizedHandler();
    throw new ApiError(res.status, data?.message ?? `Request failed (${res.status})`, data?.details ?? []);
  }
  return data;
}

/** Unauthenticated health probe for any service path prefix. */
export async function probe(prefix) {
  const started = performance.now();
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 4000);
    const res = await fetch(`${prefix}/health`, { signal: controller.signal });
    clearTimeout(timer);
    const latency = Math.round(performance.now() - started);
    if (!res.ok) return { up: false, latency, error: `HTTP ${res.status}` };
    return { up: true, latency, data: await res.json() };
  } catch {
    return { up: false, latency: null, error: "Unreachable" };
  }
}
