// Base API URL: uses VITE_API_URL in cloud/production (e.g., Render backend), or empty string for local dev proxy
export const API = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');

// Backend redirect origin — short links must point to the Spring Boot service for HTTP 302 redirects
const BACKEND_URL = (import.meta.env.VITE_BACKEND_URL || import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/+$/, '');

// ── Field normalizer ─────────────────────────────────────────────────────────
// Backend returns camelCase; frontend components expect snake_case.
export function normalizeLink(d) {
  if (!d) return d;
  const code = d.shortCode ?? d.code;
  return {
    code,
    short_url:    `${BACKEND_URL}/${code}`,
    original_url: d.originalUrl ?? d.original_url,
    clicks:       d.totalClicks ?? d.clicks ?? 0,
    enabled:      d.active      ?? (d.enabled !== undefined ? d.enabled : true),
    created_at:   d.createdAt   ?? d.created_at,
    ...d,
  };
}

// ── Short Links ──────────────────────────────────────────────────────────────

export async function listLinks() {
  const r = await fetch(`${API}/api/v1/urls`);
  const body = await r.json();
  const raw = Array.isArray(body.data) ? body.data : (Array.isArray(body) ? body : []);
  return raw.map(normalizeLink);
}

export async function shortenUrl({ url, alias }) {
  const payload = { originalUrl: url };
  if (alias && alias.trim()) payload.customAlias = alias.trim();
  const r = await fetch(`${API}/api/v1/urls/shorten`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  const body = await r.json();
  if (!r.ok) {
    const msg = body?.message || body?.error || 'Could not shorten URL.';
    const err = new Error(msg);
    err.status = r.status;
    throw err;
  }
  const d = body.data ?? body;
  const code = d.shortCode ?? d.code;
  return {
    short_url:    `${BACKEND_URL}/${code}`,
    code,
    original_url: d.originalUrl ?? d.original_url,
  };
}

export async function updateLink(code, data) {
  const r = await fetch(`${API}/api/v1/urls/${encodeURIComponent(code)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ originalUrl: data.original_url ?? data.originalUrl }),
  });
  const body = await r.json();
  return { ok: r.ok, status: r.status, data: normalizeLink(body.data ?? body) };
}

export async function deleteLink(code) {
  const r = await fetch(`${API}/api/v1/urls/${encodeURIComponent(code)}`, { method: 'DELETE' });
  return { ok: r.ok, status: r.status };
}

export async function getStats(code) {
  const r = await fetch(`${API}/api/v1/urls/${encodeURIComponent(code)}/analytics`);
  const body = await r.json();
  const d = body.data ?? body;
  return {
    clicks:      d.totalClicks ?? 0,
    total_scans: d.totalClicks ?? 0,
    analytics: {
      clicks_last_24h:        null,
      clicks_last_7d:         null,
      clicks_last_30d:        null,
      clicks_today_by_hour:   new Array(24).fill(0),
      clicks_by_country:      {},
      clicks_by_device:       {},
      clicks_by_browser:      {},
      clicks_by_os:           {},
      clicks_by_referrer:     {},
      clicks_by_utm_source:   {},
      clicks_by_utm_medium:   {},
      clicks_by_utm_campaign: {},
      bot_clicks:             0,
      recent_visits:          d.recentVisits ?? [],
    },
  };
}

// ── QR codes (backend not implemented – return graceful stubs) ───────────────
export function listQRCodes()          { return Promise.resolve([]); }
export function createQRCode(_, data) { return Promise.resolve({ ok: false, status: 501, data: { error: 'QR redirect API not implemented.' } }); }
export function updateQRCode()        { return Promise.resolve({ ok: false, status: 501, data: { error: 'Not implemented.' } }); }
export function deleteQRCode()        { return Promise.resolve({ ok: false, status: 501, data: { error: 'Not implemented.' } }); }
export function getQRStats()          { return Promise.resolve({}); }
export function adminListQRCodes()    { return Promise.resolve([]); }

// ── Auth is handled via Firebase (see src/lib/firebase.js) ───────────────────
