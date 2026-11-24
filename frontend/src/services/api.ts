// Typed client for the SnapLink backend. Every request goes through `request()`, which:
//  - attaches a fresh Firebase ID token when someone is signed in (the SDK refreshes it),
//  - parses JSON defensively (proxies can answer with HTML or plain text),
//  - turns failures into ApiError with the HTTP status.
import { auth } from '../lib/firebase';

// '' in production: the API is reached same-origin through the Vercel /api rewrite.
export const API: string = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');

// Origin that serves short links. Vercel rewrites /:code to the backend, so the current
// origin works whenever VITE_BACKEND_URL is not set.
export const SHORT_LINK_ORIGIN: string = (
  import.meta.env.VITE_BACKEND_URL || (typeof window !== 'undefined' ? window.location.origin : '')
).replace(/\/+$/, '');

export const SERVER_UNAVAILABLE_MSG =
  'Server is starting up or temporarily unavailable. Please try again in a minute.';

// ── Types ────────────────────────────────────────────────────────────────────

export type LinkStatus = 'ACTIVE' | 'SCHEDULED' | 'EXPIRED' | 'LIMIT_REACHED' | 'DISABLED';
export type StatusFilter = 'all' | 'active' | 'scheduled' | 'expired';
export type SortOrder = 'newest' | 'expiring';

export interface LinkItem {
  code: string;
  shortUrl: string;
  originalUrl: string;
  createdAt: string;
  clicks: number;
  disabled: boolean;
  status: LinkStatus;
  /** ISO time the link stops working, or null for never. */
  expiresAt: string | null;
  /** ISO time the link starts working, or null for immediately. */
  startsAt: string | null;
  /** Click limit, or null for unlimited. */
  maxClicks: number | null;
}

/** Optional limits on when and how often a link works. */
export interface LinkLifetime {
  expiresAt?: string | null;
  startsAt?: string | null;
  maxClicks?: number | null;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface LinkSummary {
  totalLinks: number;
  totalClicks: number;
  activeLinks: number;
  /** Past their expiry time or click limit. */
  expiredLinks: number;
}

export interface Visit {
  visitedAt: string;
  referrer: string | null;
  device: 'desktop' | 'mobile' | 'tablet' | 'unknown';
}

export interface LinkStats {
  totalClicks: number;
  /** 24 counts, index = hour of the current UTC day. */
  clicksTodayByHour: number[];
  recentVisits: Visit[];
}

export interface ShortenResult {
  code: string;
  shortUrl: string;
  originalUrl: string;
  expiresAt: string | null;
  startsAt: string | null;
  maxClicks: number | null;
}

export interface Me {
  uid: string;
  admin: boolean;
}

export type ReportReason = 'PHISHING' | 'MALWARE' | 'SPAM' | 'ILLEGAL' | 'OTHER';
export type ReportStatus = 'OPEN' | 'RESOLVED' | 'DISMISSED';

export interface Report {
  id: number;
  shortCode: string;
  reason: ReportReason;
  details: string | null;
  status: ReportStatus;
  createdAt: string;
  destination: string | null;
  linkDisabled: boolean | null;
}

interface Envelope<T> {
  success: boolean;
  message?: string;
  data: T;
  status: number;
}

interface RawLink {
  shortCode: string;
  originalUrl: string;
  createdAt: string;
  totalClicks: number;
  disabled: boolean;
  status?: LinkStatus;
  expiresAt?: string | null;
  startsAt?: string | null;
  maxClicks?: number | null;
}

// ── Errors ───────────────────────────────────────────────────────────────────

export class ApiError extends Error {
  readonly status: number;
  /** True when the reply did not come from the API itself (proxy page, gateway error, network). */
  readonly unavailable: boolean;

  constructor(message: string, status: number, unavailable = false) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.unavailable = unavailable;
  }
}

// ── Core request helper ──────────────────────────────────────────────────────

async function authHeader(): Promise<Record<string, string>> {
  const user = auth.currentUser;
  if (!user) return {};
  // getIdToken() returns a cached token and refreshes it shortly before it expires.
  return { Authorization: `Bearer ${await user.getIdToken()}` };
}

/** Parsed JSON body, or null when the response is not JSON. */
async function readJson(r: Response): Promise<unknown> {
  const text = await r.text().catch(() => '');
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    ...(init.body ? { 'Content-Type': 'application/json' } : {}),
    ...(await authHeader()),
    ...((init.headers as Record<string, string>) || {}),
  };
  let r: Response;
  try {
    r = await fetch(`${API}${path}`, { ...init, headers });
  } catch {
    throw new ApiError(SERVER_UNAVAILABLE_MSG, 0, true);
  }
  const body = (await readJson(r)) as Envelope<T> | null;
  if (body === null || [502, 503, 504].includes(r.status)) {
    throw new ApiError(SERVER_UNAVAILABLE_MSG, r.status, true);
  }
  if (!r.ok) {
    throw new ApiError(body?.message || `Request failed (${r.status}).`, r.status);
  }
  return body.data;
}

const json = (method: string, data?: unknown): RequestInit => ({
  method,
  body: data === undefined ? undefined : JSON.stringify(data),
});

export const shortUrlFor = (code: string): string => `${SHORT_LINK_ORIGIN}/${encodeURIComponent(code)}`;

function toLink(d: RawLink): LinkItem {
  return {
    code: d.shortCode,
    shortUrl: shortUrlFor(d.shortCode),
    originalUrl: d.originalUrl,
    createdAt: d.createdAt,
    clicks: d.totalClicks ?? 0,
    disabled: !!d.disabled,
    status: d.status ?? (d.disabled ? 'DISABLED' : 'ACTIVE'),
    expiresAt: d.expiresAt ?? null,
    startsAt: d.startsAt ?? null,
    maxClicks: d.maxClicks ?? null,
  };
}

// ── Short links ──────────────────────────────────────────────────────────────

export async function listLinks(params: {
  page?: number; size?: number; q?: string; status?: StatusFilter; sort?: SortOrder;
} = {}): Promise<Page<LinkItem>> {
  const qs = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 25),
    status: params.status ?? 'all',
    sort: params.sort ?? 'newest',
    ...(params.q ? { q: params.q } : {}),
  });
  const page = await request<Page<RawLink>>(`/api/v1/urls?${qs}`);
  return { ...page, items: page.items.map(toLink) };
}

export function getSummary(): Promise<LinkSummary> {
  return request<LinkSummary>('/api/v1/urls/summary');
}

export async function shortenUrl({ url, alias, expiresAt, startsAt, maxClicks }:
  { url: string; alias?: string } & LinkLifetime): Promise<ShortenResult> {
  const payload: Record<string, string | number> = { originalUrl: url };
  if (alias && alias.trim()) payload.customAlias = alias.trim();
  if (expiresAt) payload.expiresAt = expiresAt;
  if (startsAt) payload.startsAt = startsAt;
  if (maxClicks) payload.maxClicks = maxClicks;
  const d = await request<{ shortCode: string; originalUrl: string; expiresAt: string | null; startsAt: string | null; maxClicks: number | null }>(
    '/api/v1/urls/shorten', json('POST', payload));
  return {
    code: d.shortCode, shortUrl: shortUrlFor(d.shortCode), originalUrl: d.originalUrl,
    expiresAt: d.expiresAt ?? null, startsAt: d.startsAt ?? null, maxClicks: d.maxClicks ?? null,
  };
}

export interface LinkChanges {
  originalUrl?: string;
  customAlias?: string;
  expiresAt?: string;
  clearExpiresAt?: boolean;
  startsAt?: string;
  clearStartsAt?: boolean;
  maxClicks?: number;
  clearMaxClicks?: boolean;
}

/** Deletes all of the caller's expired links; returns how many were deleted. */
export async function deleteExpiredLinks(): Promise<number> {
  const d = await request<{ deleted: number }>('/api/v1/urls?status=expired', { method: 'DELETE' });
  return d.deleted;
}

export async function updateLink(code: string, changes: LinkChanges): Promise<LinkItem> {
  const d = await request<RawLink>(`/api/v1/urls/${encodeURIComponent(code)}`, json('PUT', changes));
  return toLink(d);
}

export async function deleteLink(code: string): Promise<void> {
  await request<void>(`/api/v1/urls/${encodeURIComponent(code)}`, { method: 'DELETE' });
}

export async function getStats(code: string): Promise<LinkStats> {
  const d = await request<LinkStats>(`/api/v1/urls/${encodeURIComponent(code)}/analytics`);
  return {
    totalClicks: d.totalClicks ?? 0,
    clicksTodayByHour: Array.isArray(d.clicksTodayByHour) && d.clicksTodayByHour.length === 24
      ? d.clicksTodayByHour
      : new Array(24).fill(0),
    recentVisits: Array.isArray(d.recentVisits) ? d.recentVisits : [],
  };
}

export function getMe(): Promise<Me> {
  return request<Me>('/api/v1/me');
}

// ── Abuse reports ────────────────────────────────────────────────────────────

export async function reportLink(shortCode: string, reason: ReportReason, details?: string): Promise<void> {
  await request<void>('/api/v1/reports', json('POST', { shortCode, reason, details: details || undefined }));
}

export function listReports(status: ReportStatus = 'OPEN', page = 0): Promise<Page<Report>> {
  return request<Page<Report>>(`/api/v1/admin/reports?status=${status}&page=${page}&size=25`);
}

export async function resolveReport(id: number, status: Exclude<ReportStatus, 'OPEN'>, disableLink: boolean): Promise<void> {
  await request<void>(`/api/v1/admin/reports/${id}/resolve?status=${status}&disableLink=${disableLink}`, { method: 'POST' });
}

export async function setLinkDisabled(code: string, disabled: boolean): Promise<void> {
  await request<void>(`/api/v1/admin/urls/${encodeURIComponent(code)}/${disabled ? 'disable' : 'enable'}`, { method: 'POST' });
}

/** Extracts the short code from a pasted short link, or returns the input trimmed. */
export function extractShortCode(input: string): string {
  const s = input.trim();
  try {
    const u = new URL(s);
    return decodeURIComponent(u.pathname.replace(/^\/+/, '').split('/')[0] || '');
  } catch {
    return s.replace(/^\/+/, '');
  }
}
