import { useEffect, useState } from 'react';
import { C } from '../constants/theme';
import { useBackendStatus, wakeBackend } from '../hooks/useBackendStatus';

const EXPECTED_COLD_START_S = 50;

function useElapsed(startedAt, active) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    if (!active) return;
    const t = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(t);
  }, [active]);
  return startedAt ? Math.max(0, Math.floor((now - startedAt) / 1000)) : 0;
}

const DOT = {
  checking: C.muted,
  waking: C.warning,
  up: C.accent3,
  down: C.error,
  idle: C.muted,
};

/** Small status pill for the top bar. */
export function BackendStatusPill() {
  const { status } = useBackendStatus();
  const label = {
    idle: 'Connecting',
    checking: 'Connecting',
    waking: 'Server waking up',
    up: 'Online',
    down: 'Offline',
  }[status];
  const pulsing = status === 'checking' || status === 'waking';
  return (
    <span
      title={status === 'down' ? 'Click to retry' : undefined}
      onClick={status === 'down' ? wakeBackend : undefined}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: 7,
        fontFamily: C.mono, fontSize: 11, color: C.muted,
        padding: '4px 10px', borderRadius: 9999,
        border: '1px solid rgba(255,255,255,0.08)', background: 'rgba(255,255,255,0.03)',
        cursor: status === 'down' ? 'pointer' : 'default', whiteSpace: 'nowrap',
      }}
    >
      <span style={{
        width: 7, height: 7, borderRadius: '50%', background: DOT[status],
        boxShadow: `0 0 8px ${DOT[status]}`,
        animation: pulsing ? 'statusPulse 1.2s ease-in-out infinite' : 'none',
      }} />
      {label}
    </span>
  );
}

/** Banner explaining the Render cold start, with a live timer. */
export function BackendStatusBanner() {
  const { status, startedAt, readyAt, wasCold } = useBackendStatus();
  const waking = status === 'waking';
  const elapsed = useElapsed(startedAt, waking);
  const [showReady, setShowReady] = useState(false);

  // After a cold start, show "up and running" briefly, then hide.
  useEffect(() => {
    if (status === 'up' && wasCold) {
      setShowReady(true);
      const t = setTimeout(() => setShowReady(false), 4000);
      return () => clearTimeout(t);
    }
  }, [status, wasCold]);

  if (!waking && status !== 'down' && !showReady) return null;

  const tookS = readyAt && startedAt ? Math.round((readyAt - startedAt) / 1000) : null;
  // Asymptotic progress so the bar keeps moving but never claims 100% before the server answers.
  const pct = status === 'up' ? 100 : Math.min(95, Math.round((1 - Math.exp(-elapsed / (EXPECTED_COLD_START_S / 2))) * 100));

  const tone = status === 'up' ? C.accent3 : status === 'down' ? C.error : C.warning;

  return (
    <div
      role="status"
      aria-live="polite"
      className="gcard"
      style={{
        position: 'relative', overflow: 'hidden', padding: '14px 18px',
        display: 'flex', alignItems: 'center', gap: 14,
        borderColor: `${tone}40`, animation: 'fadeUp .3s both',
      }}
    >
      <span style={{
        width: 10, height: 10, borderRadius: '50%', flexShrink: 0, background: tone,
        boxShadow: `0 0 12px ${tone}`,
        animation: waking ? 'statusPulse 1.2s ease-in-out infinite' : 'none',
      }} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontFamily: C.display, fontWeight: 700, fontSize: 14, color: C.text }}>
          {waking && 'Waking up the server…'}
          {status === 'up' && "We're up and running!"}
          {status === 'down' && "Couldn't reach the server"}
        </div>
        <div style={{ fontFamily: C.mono, fontSize: 11.5, color: C.muted, marginTop: 3 }}>
          {waking && `Our backend sleeps when idle and takes up to a minute to start. ${elapsed}s elapsed — you can fill in your link meanwhile.`}
          {status === 'up' && (tookS != null ? `Server was ready in ${tookS}s. You can shorten links now.` : 'You can shorten links now.')}
          {status === 'down' && 'The server did not respond after several attempts.'}
        </div>
      </div>
      {status === 'down' && (
        <button className="btn-g btn-sm" onClick={wakeBackend}>Retry</button>
      )}
      {waking && (
        <span style={{ fontFamily: C.mono, fontSize: 13, color: tone, fontVariantNumeric: 'tabular-nums' }}>{elapsed}s</span>
      )}
      {status !== 'down' && (
        <div style={{ position: 'absolute', left: 0, bottom: 0, height: 2, width: `${pct}%`, background: tone, transition: 'width .5s ease' }} />
      )}
    </div>
  );
}
