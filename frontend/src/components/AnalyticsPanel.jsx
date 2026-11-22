import { useState, useEffect, useCallback } from 'react';
import { C } from '../constants/theme';
import { getStats } from '../services/api';

function HourlyBarChart({ data }) {
  const max = Math.max(...data, 1);
  const [tip, setTip] = useState(null);
  const total = data.reduce((a, b) => a + b, 0);
  return (
    <div style={{ position: 'relative' }}>
      {tip !== null && (
        <div style={{ position: 'absolute', top: -28, left: `${((tip + 0.5) / 24) * 100}%`, transform: 'translateX(-50%)', background: C.solid, border: `1px solid ${C.border2}`, borderRadius: 5, padding: '2px 8px', fontFamily: C.mono, fontSize: 11, color: C.text, whiteSpace: 'nowrap', pointerEvents: 'none', zIndex: 10 }}>
          {String(tip).padStart(2, '0')}:00 UTC · {data[tip]}
        </div>
      )}
      <div role="img" aria-label={`${total} clicks today, by hour (UTC)`}
        style={{ display: 'flex', gap: 3, height: 52, alignItems: 'flex-end', paddingTop: 4 }}>
        {data.map((v, i) => (
          <div key={i} onMouseEnter={() => setTip(i)} onMouseLeave={() => setTip(null)}
            style={{
              flex: 1, background: tip === i ? 'rgba(108,99,255,0.7)' : (v ? C.accent : 'rgba(255,255,255,0.06)'),
              height: `${Math.max(3, (v / max) * 100)}%`, borderRadius: 2,
              opacity: v > 0 ? 1 : 0.25, transition: 'height .3s, opacity .3s, background .1s',
            }} />
        ))}
      </div>
    </div>
  );
}

const label = { fontFamily: C.mono, fontSize: 10, color: C.muted, textTransform: 'uppercase', letterSpacing: '0.08em' };

/** Click analytics for one of the caller's links. */
export function AnalyticsPanel({ code }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    if (!code) return;
    setLoading(true);
    setError('');
    getStats(code)
      .then(setData)
      .catch(err => setError(err.message || 'Could not load analytics.'))
      .finally(() => setLoading(false));
  }, [code]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <div style={{ padding: '12px 0', color: C.muted, fontFamily: C.mono, fontSize: 13 }}>Loading analytics…</div>;
  if (error) {
    return (
      <div role="alert" style={{ padding: '12px 0', display: 'flex', gap: 12, alignItems: 'center', fontFamily: C.mono, fontSize: 12, color: C.error }}>
        {error}
        <button className="btn-g btn-sm" onClick={load}>Retry</button>
      </div>
    );
  }
  if (!data) return null;

  return (
    <div style={{ padding: '14px 0 6px' }}>
      <div style={{ ...label, fontSize: 11, marginBottom: 16 }}>
        Total clicks: <span style={{ color: C.text, fontWeight: 700, fontSize: 20 }}>{data.totalClicks.toLocaleString()}</span>
      </div>

      <div style={{ marginBottom: 14 }}>
        <div style={{ ...label, marginBottom: 6 }}>Clicks today by hour (UTC)</div>
        <HourlyBarChart data={data.clicksTodayByHour} />
      </div>

      {data.recentVisits.length > 0 && (
        <div style={{ marginTop: 10 }}>
          <div style={{ ...label, marginBottom: 8 }}>Recent visits</div>
          <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 4 }}>
            {data.recentVisits.map((v, i) => (
              <li key={i} style={{ display: 'flex', gap: 16, flexWrap: 'wrap', fontFamily: C.mono, fontSize: 11, color: C.muted, padding: '4px 0', borderBottom: `1px solid ${C.border}` }}>
                <span style={{ color: C.text }}>{new Date(v.visitedAt).toLocaleString()}</span>
                <span>{v.device}</span>
                <span>{v.referrer || 'direct'}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {data.totalClicks === 0 && (
        <div style={{ fontFamily: C.mono, fontSize: 12, color: C.muted }}>No clicks yet.</div>
      )}
    </div>
  );
}
