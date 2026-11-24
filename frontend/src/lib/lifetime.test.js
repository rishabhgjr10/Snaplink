import { describe, expect, it } from 'vitest';
import { choiceFromIso, describeLink, expiryToIso, formatDuration, isoToLocalInput, localInputToIso } from './lifetime';

const NOW = Date.parse('2026-11-24T12:00:00Z');
const HOUR = 3600_000;
const base = { clicks: 0, maxClicks: null, expiresAt: null, startsAt: null, disabled: false, status: 'ACTIVE' };

describe('expiry presets', () => {
  it('turns presets into ISO times relative to now', () => {
    expect(expiryToIso({ preset: 'never' }, NOW)).toBeNull();
    expect(expiryToIso({ preset: '3h' }, NOW)).toBe('2026-11-24T15:00:00.000Z');
    expect(expiryToIso({ preset: '1d' }, NOW)).toBe('2026-11-25T12:00:00.000Z');
    expect(expiryToIso({ preset: '30d' }, NOW)).toBe('2026-12-24T12:00:00.000Z');
  });

  it('round-trips custom local date-times', () => {
    const iso = '2026-12-01T09:30:00.000Z';
    expect(localInputToIso(isoToLocalInput(iso))).toBe(iso);
    expect(choiceFromIso(iso)).toEqual({ preset: 'custom', custom: isoToLocalInput(iso) });
    expect(choiceFromIso(null)).toEqual({ preset: 'never', custom: '' });
    expect(localInputToIso('')).toBeNull();
  });
});

describe('describeLink', () => {
  it('counts down to expiry and warns within a day', () => {
    expect(describeLink({ ...base, expiresAt: new Date(NOW + 3 * HOUR + 12 * 60000).toISOString() }, NOW))
      .toMatchObject({ label: 'Expires in 3h 12m', tone: 'warn' });
    expect(describeLink({ ...base, expiresAt: new Date(NOW + 50 * HOUR).toISOString() }, NOW))
      .toMatchObject({ label: 'Expires in 2d 2h', tone: 'ok' });
  });

  it('reports expired, scheduled, limit reached and disabled links', () => {
    expect(describeLink({ ...base, expiresAt: new Date(NOW - 2 * HOUR).toISOString() }, NOW))
      .toMatchObject({ label: 'Expired', tone: 'off', detail: '2h ago' });
    expect(describeLink({ ...base, startsAt: new Date(NOW + 5 * HOUR).toISOString() }, NOW))
      .toMatchObject({ label: 'Starts in 5h', tone: 'warn' });
    expect(describeLink({ ...base, clicks: 100, maxClicks: 100 }, NOW))
      .toMatchObject({ label: 'Limit reached', tone: 'off', detail: '100 / 100 clicks' });
    expect(describeLink({ ...base, status: 'DISABLED', disabled: true }, NOW)).toMatchObject({ label: 'Disabled' });
    expect(describeLink({ ...base, clicks: 12, maxClicks: 100 }, NOW))
      .toMatchObject({ label: 'Active', detail: '12 / 100 clicks' });
  });

  it('formats durations', () => {
    expect(formatDuration(30_000)).toBe('less than a minute');
    expect(formatDuration(45 * 60000)).toBe('45m');
    expect(formatDuration(3 * 24 * HOUR)).toBe('3d');
  });
});
