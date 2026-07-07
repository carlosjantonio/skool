import type { ProblemDetail, TokenResponse } from './types';

const BASE = import.meta.env.VITE_API_BASE_URL || '';

interface ApiOptions extends RequestInit {
  auth?: boolean;
}

let accessToken: string | null = null;
let refreshToken: string | null = null;
let onSessionEnded: (() => void) | null = null;

export function setTokens(t: Pick<TokenResponse, 'accessToken' | 'refreshToken'> | null) {
  accessToken = t?.accessToken ?? null;
  refreshToken = t?.refreshToken ?? null;
  if (t) {
    localStorage.setItem('skool.refresh', t.refreshToken);
  } else {
    localStorage.removeItem('skool.refresh');
  }
}

export function loadPersistedRefresh(): string | null {
  return localStorage.getItem('skool.refresh');
}

export function onSessionEnd(cb: () => void) { onSessionEnded = cb; }

export class ApiError extends Error {
  constructor(public status: number, public problem?: ProblemDetail) {
    super(problem?.detail || problem?.title || `HTTP ${status}`);
  }
}

async function doFetch<T>(path: string, opts: ApiOptions): Promise<T> {
  const headers = new Headers(opts.headers);
  if (!headers.has('Content-Type') && opts.body) headers.set('Content-Type', 'application/json');
  if (!headers.has('Accept-Language')) headers.set('Accept-Language', 'pt-AO');
  if (opts.auth !== false && accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  const res = await fetch(BASE + path, { ...opts, headers });

  if (res.status === 204) return undefined as T;

  const text = await res.text();
  const body = text ? JSON.parse(text) : null;

  if (!res.ok) throw new ApiError(res.status, body as ProblemDetail);
  return body as T;
}

export async function api<T>(path: string, opts: ApiOptions = {}): Promise<T> {
  try {
    return await doFetch<T>(path, opts);
  } catch (err) {
    if (err instanceof ApiError && err.status === 401 && opts.auth !== false && refreshToken) {
      // Try one refresh, then retry.
      const refreshed = await tryRefresh();
      if (refreshed) {
        return doFetch<T>(path, opts);
      }
      onSessionEnded?.();
    }
    throw err;
  }
}

async function tryRefresh(): Promise<boolean> {
  if (!refreshToken) return false;
  try {
    const res = await fetch(BASE + '/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const data = (await res.json()) as TokenResponse;
    setTokens(data);
    return true;
  } catch {
    return false;
  }
}
