import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const currentUser: { getIdToken: () => Promise<string> } | null = { getIdToken: vi.fn(async () => 'fresh-token') };
let signedIn = true;

vi.mock('../lib/firebase', () => ({
  auth: {
    get currentUser() { return signedIn ? currentUser : null; },
  },
}));

import {
  ApiError, SERVER_UNAVAILABLE_MSG, deleteLink, extractShortCode, getStats, listLinks, shortenUrl, updateLink,
} from './api';

function respond(status: number, body: string) {
  return vi.fn(async () => new Response(body, { status }));
}

describe('api client', () => {
  beforeEach(() => { signedIn = true; });
  afterEach(() => { vi.unstubAllGlobals(); });

  it('sends a fresh bearer token when signed in', async () => {
    const fetchMock = respond(200, JSON.stringify({ success: true, data: { items: [], page: 0, size: 25, totalItems: 0, totalPages: 0 } }));
    vi.stubGlobal('fetch', fetchMock);
    await listLinks({ q: 'abc', status: 'expired', sort: 'expiring' });
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toContain('/api/v1/urls?page=0&size=25&status=expired&sort=expiring&q=abc');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer fresh-token');
  });

  it('sends no Authorization header when signed out', async () => {
    signedIn = false;
    const fetchMock = respond(200, JSON.stringify({ success: true, data: { shortCode: 'abc1234', originalUrl: 'https://x.example' } }));
    vi.stubGlobal('fetch', fetchMock);
    const result = await shortenUrl({ url: 'https://x.example' });
    const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect((init.headers as Record<string, string>).Authorization).toBeUndefined();
    expect(result.code).toBe('abc1234');
    expect(result.shortUrl.endsWith('/abc1234')).toBe(true);
  });

  it('maps Render plain-text 429 to an "unavailable" error', async () => {
    vi.stubGlobal('fetch', respond(429, 'Too Many Requests'));
    await expect(shortenUrl({ url: 'https://x.example' })).rejects.toMatchObject({
      status: 429, unavailable: true, message: SERVER_UNAVAILABLE_MSG,
    });
  });

  it('keeps the API error message for JSON errors', async () => {
    vi.stubGlobal('fetch', respond(409, JSON.stringify({ success: false, message: 'Custom alias taken', status: 409 })));
    const err = await shortenUrl({ url: 'https://x.example', alias: 'taken' }).catch(e => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(409);
    expect(err.unavailable).toBe(false);
    expect(err.message).toBe('Custom alias taken');
  });

  it('treats network failures and gateway errors as unavailable', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch'); }));
    await expect(deleteLink('abc')).rejects.toMatchObject({ unavailable: true, status: 0 });
    vi.stubGlobal('fetch', respond(503, '{"message":"down"}'));
    await expect(deleteLink('abc')).rejects.toMatchObject({ unavailable: true, status: 503 });
  });

  it('url-encodes short codes in paths', async () => {
    const fetchMock = respond(200, JSON.stringify({ success: true, data: { shortCode: 'new', originalUrl: 'https://y', createdAt: '', totalClicks: 3, disabled: false } }));
    vi.stubGlobal('fetch', fetchMock);
    const link = await updateLink('a b', { customAlias: 'new' });
    expect((fetchMock.mock.calls[0] as unknown as [string])[0]).toContain('/api/v1/urls/a%20b');
    expect(link).toMatchObject({ code: 'new', clicks: 3 });
  });

  it('normalises analytics responses', async () => {
    vi.stubGlobal('fetch', respond(200, JSON.stringify({ success: true, data: { totalClicks: 2 } })));
    const stats = await getStats('abc');
    expect(stats.clicksTodayByHour).toHaveLength(24);
    expect(stats.recentVisits).toEqual([]);
  });

  it('extracts short codes from pasted links', () => {
    expect(extractShortCode('https://bauna.xyz/abc1234')).toBe('abc1234');
    expect(extractShortCode(' https://bauna.xyz/My-Promo?utm=1 ')).toBe('My-Promo');
    expect(extractShortCode('/abc')).toBe('abc');
    expect(extractShortCode('abc')).toBe('abc');
  });
});
