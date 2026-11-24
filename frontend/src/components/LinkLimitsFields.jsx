import { useId, useState } from 'react';
import { C } from '../constants/theme';
import { MAX_LIFETIME_DAYS, isoToLocalInput } from '../lib/lifetime';

/**
 * Collapsible "Start time" and "Click limit" fields.
 * startsAt: datetime-local string ('' = immediately); maxClicks: string ('' = unlimited).
 */
export function LinkLimitsFields({ startsAt, onStartsAt, maxClicks, onMaxClicks, defaultOpen = false }) {
  const id = useId();
  const [open, setOpen] = useState(defaultOpen || !!startsAt || !!maxClicks);
  const maxStart = isoToLocalInput(new Date(Date.now() + MAX_LIFETIME_DAYS * 864e5).toISOString());

  return (
    <div>
      <button type="button" className="accbtn" aria-expanded={open} aria-controls={`${id}-panel`} onClick={() => setOpen(o => !o)}>
        <span>Advanced: start time & click limit</span>
        <span aria-hidden="true" style={{ fontSize: 10 }}>{open ? '▲' : '▼'}</span>
      </button>
      {open && (
        <div id={`${id}-panel`} style={{ display: 'flex', flexDirection: 'column', gap: 14, marginTop: 12, animation: 'expandD .2s both' }}>
          <div>
            <label className="field-label" htmlFor={`${id}-start`}>Start working at</label>
            <div style={{ display: 'flex', gap: 8 }}>
              <input id={`${id}-start`} className="inp" type="datetime-local" value={startsAt} max={maxStart}
                onChange={e => onStartsAt(e.target.value)} />
              {startsAt && <button type="button" className="btn-g btn-sm" onClick={() => onStartsAt('')}>Clear</button>}
            </div>
            <p style={{ fontFamily: C.mono, fontSize: 11, color: C.muted, marginTop: 6 }}>
              Leave empty to start immediately. Before this time visitors see a "not active yet" page.
            </p>
          </div>
          <div>
            <label className="field-label" htmlFor={`${id}-max`}>Stop after this many clicks</label>
            <input id={`${id}-max`} className="inp" type="number" inputMode="numeric" min={1} max={1000000} step={1}
              placeholder="Unlimited" value={maxClicks} onChange={e => onMaxClicks(e.target.value)} />
            <p style={{ fontFamily: C.mono, fontSize: 11, color: C.muted, marginTop: 6 }}>
              Counts real visitors only (bots and repeat clicks within 30 seconds are ignored).
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
