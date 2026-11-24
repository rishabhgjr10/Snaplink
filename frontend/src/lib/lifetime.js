// Link lifetime helpers: expiry presets, date conversions, human-readable status.
import { useEffect, useState } from 'react';

const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;

/** Expiry presets offered when creating or editing a link. */
export const EXPIRY_PRESETS = [
  { id: 'never', label: 'Never' },
  { id: '1h', label: '1 hour', ms: HOUR },
  { id: '3h', label: '3 hours', ms: 3 * HOUR },
  { id: '6h', label: '6 hours', ms: 6 * HOUR },
  { id: '12h', label: '12 hours', ms: 12 * HOUR },
  { id: '1d', label: '1 day', ms: DAY },
  { id: '7d', label: '7 days', ms: 7 * DAY },
  { id: '30d', label: '30 days', ms: 30 * DAY },
  { id: 'custom', label: 'Custom…' },
];

/** Signed-out users always get an expiry, of at most this long (matches the backend). */
export const ANONYMOUS_MAX_DAYS = 30;
export const MAX_LIFETIME_DAYS = 365;
export const MIN_LIFETIME_MINUTES = 5;

/** Expiry choice → ISO string (or null for "never"). Custom uses the local date-time input value. */
export function expiryToIso(choice, now = Date.now()) {
  if (!choice || choice.preset === 'never') return null;
  if (choice.preset === 'custom') return localInputToIso(choice.custom);
  const preset = EXPIRY_PRESETS.find(p => p.id === choice.preset);
  return preset?.ms ? new Date(now + preset.ms).toISOString() : null;
}

/** Initial picker state for an existing expiry (edit dialog). */
export function choiceFromIso(iso) {
  return iso ? { preset: 'custom', custom: isoToLocalInput(iso) } : { preset: 'never', custom: '' };
}

/** ISO → value for <input type="datetime-local"> in the viewer's time zone. */
export function isoToLocalInput(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** <input type="datetime-local"> value (local time) → ISO, or null if empty/invalid. */
export function localInputToIso(value) {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
}

/** "3d 4h", "5h 12m", "12m", "less than a minute". */
export function formatDuration(ms) {
  const abs = Math.abs(ms);
  const d = Math.floor(abs / DAY);
  const h = Math.floor((abs % DAY) / HOUR);
  const m = Math.floor((abs % HOUR) / 60000);
  if (d > 0) return h > 0 ? `${d}d ${h}h` : `${d}d`;
  if (h > 0) return m > 0 ? `${h}h ${m}m` : `${h}h`;
  if (m > 0) return `${m}m`;
  return 'less than a minute';
}

export function formatDateTime(iso) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

/**
 * Human-readable state of a link at `now`.
 * tone: "ok" (green), "warn" (amber, expiring within a day or scheduled), "off" (red/grey).
 */
export function describeLink(link, now = Date.now()) {
  const expires = link.expiresAt ? new Date(link.expiresAt).getTime() : null;
  const starts = link.startsAt ? new Date(link.startsAt).getTime() : null;
  const limitText = link.maxClicks ? `${link.clicks.toLocaleString()} / ${link.maxClicks.toLocaleString()} clicks` : null;

  if (link.status === 'DISABLED' || link.disabled) {
    return { label: 'Disabled', tone: 'off', detail: 'Taken down by a moderator' };
  }
  if (link.status === 'EXPIRED' || (expires !== null && expires <= now)) {
    return { label: 'Expired', tone: 'off', detail: expires !== null ? `${formatDuration(now - expires)} ago` : null };
  }
  if (link.status === 'LIMIT_REACHED' || (link.maxClicks && link.clicks >= link.maxClicks)) {
    return { label: 'Limit reached', tone: 'off', detail: limitText };
  }
  if (starts !== null && starts > now) {
    return { label: `Starts in ${formatDuration(starts - now)}`, tone: 'warn', detail: limitText };
  }
  if (expires !== null) {
    return {
      label: `Expires in ${formatDuration(expires - now)}`,
      tone: expires - now < DAY ? 'warn' : 'ok',
      detail: limitText,
    };
  }
  return { label: 'Active', tone: 'ok', detail: limitText };
}

/** Re-renders every `intervalMs` so countdowns stay current. */
export function useNow(intervalMs = 15000) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), intervalMs);
    return () => clearInterval(t);
  }, [intervalMs]);
  return now;
}
