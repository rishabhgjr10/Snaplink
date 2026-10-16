import { useState, useEffect } from 'react';
import { C } from '../constants/theme';
import { getStats } from '../services/api';

function HourlyBarChart({ data }) {
  const max = Math.max(...data, 1);
  const [tip, setTip] = useState(null);
  return (
    <div style={{ position: 'relative' }}>
      {tip !== null && (
        <div style={{ position: 'absolute', top: -28, left: `${(tip / 24) * 100}%`, transform: 'translateX(-50%)', background: C.card, border: `1px solid ${C.border2}`, borderRadius: 5, padding: '2px 8px', fontFamily: C.mono, fontSize: 11, color: C.text, whiteSpace: 'nowrap', pointerEvents: 'none', zIndex: 10 }}>
          {tip}:00 · {data[tip]}
        </div>
      )}
      <div style={{ display: 'flex', gap: 3, height: 52, alignItems: 'flex-end', paddingTop: 4 }}>
        {data.map((v, i) => (
          <div key={i} onMouseEnter={() => setTip(i)} onMouseLeave={() => setTip(null)}
            style={{
              flex: 1, background: tip === i ? 'rgba(108,99,255,0.7)' : (v ? C.accent : 'rgba(255,255,255,0.06)'),
              height: `${Math.max(3, (v / max) * 100)}%`, borderRadius: 2,
              opacity: v > 0 ? 1 : 0.25, transition: 'height .3s, opacity .3s, background .1s',
              cursor: 'default',
            }} />
        ))}
      </div>
    </div>
  );
}

export function AnalyticsPanel({ code, token, statsEndpoint }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!code) return;
    setLoading(true);
    getStats(code)
      .then(d => { setData(d); setLoading(false); })
      .catch(() => setLoading(false));
  }, [code]);

  if (loading) return <div style={{ padding: '12px 0', color: C.muted, fontFamily: C.mono, fontSize: 13 }}>Loading analytics…</div>;
  if (!data) return null;

  const totalClicks = data.clicks ?? 0;
  const recentVisits = data.analytics?.recent_visits ?? [];
  const hourlyData = data.analytics?.clicks_today_by_hour ?? new Array(24).fill(0);

  return (
    <div style={{ padding: '14px 0 6px' }}>
      {/* Total clicks */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16, flexWrap: 'wrap' }}>
        <div style={{ fontFamily: C.mono, fontSize: 11, color: C.muted, textTransform: 'uppercase', letterSpacing: '.06em' }}>
          Total clicks: <span style={{ color: C.text, fontWeight: 700, fontSize: 20 }}>{totalClicks.toLocaleString()}</span>
        </div>
      </div>

      {/* Hourly chart placeholder */}
      <div style={{ marginBottom: 14 }}>
        <div style={{ fontFamily: C.mono, fontSize: 10, color: C.muted, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 6 }}>Clicks today by hour (UTC)</div>
        <HourlyBarChart data={hourlyData} />
      </div>

      {/* Recent visits table */}
      {recentVisits.length > 0 && (
        <div style={{ marginTop: 10 }}>
          <div style={{ fontFamily: C.mono, fontSize: 10, color: C.muted, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 8 }}>Recent visits</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {recentVisits.slice(0, 8).map((v, i) => (
              <div key={i} style={{ display: 'flex', gap: 16, fontFamily: C.mono, fontSize: 11, color: C.muted, padding: '4px 0', borderBottom: `1px solid ${C.border}` }}>
                <span style={{ color: C.text }}>{v.visitedAt ? new Date(v.visitedAt).toLocaleString() : '—'}</span>
                {v.ipAddress && <span>{v.ipAddress}</span>}
                {v.userAgent && <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 200 }}>{v.userAgent}</span>}
              </div>
            ))}
          </div>
        </div>
      )}

      {totalClicks === 0 && recentVisits.length === 0 && (
        <div style={{ fontFamily: C.mono, fontSize: 12, color: C.muted }}>No clicks yet.</div>
      )}
    </div>
  );
}



