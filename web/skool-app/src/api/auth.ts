import { api, setTokens } from './client';
import type { TokenResponse } from './types';

export async function login(email: string, password: string): Promise<TokenResponse> {
  const res = await api<TokenResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
    auth: false,
  });
  setTokens(res);
  return res;
}

export async function refresh(refreshToken: string): Promise<TokenResponse> {
  const res = await api<TokenResponse>('/api/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
    auth: false,
  });
  setTokens(res);
  return res;
}

export async function logout(refreshToken: string): Promise<void> {
  try {
    await api<void>('/api/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
      auth: false,
    });
  } finally {
    setTokens(null);
  }
}
