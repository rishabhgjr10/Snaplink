import { useId } from 'react';
import { C } from '../constants/theme';
import { ANONYMOUS_MAX_DAYS, EXPIRY_PRESETS, MAX_LIFETIME_DAYS, isoToLocalInput } from '../lib/lifetime';

/**
 * Choose when a link stops working.
 * value: { preset: 'never' | '1h' | … | 'custom', custom: 'YYYY-MM-DDTHH:mm' }
 * anonymous: signed-out users cannot pick "Never" or more than 30 days.
 * variant: "chips" (create/edit panels) or "select" (compact, landing page).
 */
export function ExpiryPicker({ value, onChange, anonymous = false, variant = 'chips', label = 'Expires after' }) {
  const id = useId();
  const maxDays = anonymous ? ANONYMOUS_MAX_DAYS : MAX_LIFETIME_DAYS;
  const presets = EXPIRY_PRESETS.filter(p => !(anonymous && p.id === 'never') && (!p.ms || p.ms <= maxDays * 864e5));
  const maxCustom = isoToLocalInput(new Date(Date.now() + maxDays * 864e5).toISOString());
  const set = (patch) => onChange({ ...value, ...patch });

  const custom = value.preset === 'custom' && (
    <input
      id={`${id}-custom`}
      className="inp"
      type="datetime-local"
      aria-label="Custom expiry date and time"
      value={value.custom}
      // No `min`: an expired link's current (past) expiry must not block saving other edits.
      // The server rejects expiries less than 5 minutes away with a clear message.
      max={maxCustom}
      onChange={e => set({ custom: e.target.value })}
      required
      style={{ marginTop: 8 }}
    />
  );

  if (variant === 'select') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 4, flex: '0 0 auto' }}>
        <label className="sr-only" htmlFor={`${id}-select`}>{label}</label>
        <select id={`${id}-select`} className="inp" value={value.preset} onChange={e => set({ preset: e.target.value })}
          title={label} style={{ width: 'auto', paddingRight: 28 }}>
          {presets.map(p => (
            <option key={p.id} value={p.id}>{p.id === 'never' ? 'Never expires' : p.id === 'custom' ? 'Custom…' : `Expires in ${p.label}`}</option>
          ))}
        </select>
        {custom}
      </div>
    );
  }

  return (
    <fieldset style={{ border: 'none', padding: 0, margin: 0 }}>
      <legend className="field-label">{label}</legend>
      <div role="radiogroup" aria-label={label} style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        {presets.map(p => {
          const selected = value.preset === p.id;
          return (
            <button
              key={p.id}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => set({ preset: p.id })}
              style={{
                fontFamily: C.mono, fontSize: 12, padding: '6px 12px', borderRadius: 9999, cursor: 'pointer',
                border: `1px solid ${selected ? C.accent : 'rgba(255,255,255,0.12)'}`,
                background: selected ? 'rgba(108,99,255,0.18)' : 'rgba(255,255,255,0.04)',
                color: selected ? C.text : C.muted, transition: 'all .15s',
              }}
            >
              {p.label}
            </button>
          );
        })}
      </div>
      {custom}
      {anonymous && (
        <p style={{ fontFamily: C.mono, fontSize: 11, color: C.muted, marginTop: 6 }}>
          Links made without signing in last at most {ANONYMOUS_MAX_DAYS} days.
        </p>
      )}
    </fieldset>
  );
}
