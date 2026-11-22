import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { C } from '../constants/theme';
import { NavBar } from '../layouts/MainLayout';
import { listReports, resolveReport, setLinkDisabled, shortUrlFor } from '../services/api';

const STATUSES = ['OPEN', 'RESOLVED', 'DISMISSED'];

/** Moderation: review abuse reports and take links down. Access is checked by the backend. */
export function AdminPage({ toast }) {
  const [status, setStatus] = useState('OPEN');
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(null);

  const load = useCallback(async () => {
    setError('');
    try {
      setData(await listReports(status));
    } catch (err) {
      setError(err.message || 'Could not load reports.');
    }
  }, [status]);

  useEffect(() => { setData(null); load(); }, [load]);

  async function act(report, action) {
    setBusy(report.id);
    try {
      if (action === 'disable') await resolveReport(report.id, 'RESOLVED', true);
      if (action === 'dismiss') await resolveReport(report.id, 'DISMISSED', false);
      if (action === 'enable') await setLinkDisabled(report.shortCode, false);
      toast('Done.', 'success');
      await load();
    } catch (err) {
      toast(err.message || 'Action failed.', 'error');
    } finally {
      setBusy(null);
    }
  }

  return (
    <div style={{ minHeight: '100vh', color: C.text }}>
      <NavBar />
      <main style={{ maxWidth: 1000, margin: '0 auto', padding: '96px 16px 48px', display: 'flex', flexDirection: 'column', gap: 18 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
          <h1 style={{ fontFamily: C.display, fontWeight: 800, fontSize: 26 }}>Abuse reports</h1>
          <Link to="/dashboard" className="btn-g btn-sm">← Dashboard</Link>
        </div>

        <div role="tablist" aria-label="Report status" style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {STATUSES.map(s => (
            <button key={s} role="tab" aria-selected={status === s}
              className={status === s ? 'btn-p btn-sm' : 'btn-g btn-sm'} onClick={() => setStatus(s)}>
              {s.toLowerCase()}
            </button>
          ))}
        </div>

        {error && (
          <div role="alert" style={{ fontFamily: C.mono, fontSize: 13, color: C.error, display: 'flex', gap: 12, alignItems: 'center' }}>
            {error} <button className="btn-g btn-sm" onClick={load}>Retry</button>
          </div>
        )}
        {!error && !data && <p style={{ fontFamily: C.mono, fontSize: 13, color: C.muted }}>Loading…</p>}
        {data && data.items.length === 0 && (
          <p style={{ fontFamily: C.mono, fontSize: 13, color: C.muted }}>No {status.toLowerCase()} reports.</p>
        )}

        {data?.items.map(r => (
          <article key={r.id} className="gcard" style={{ padding: '16px 18px', display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap', fontFamily: C.mono, fontSize: 13 }}>
              <a href={shortUrlFor(r.shortCode)} target="_blank" rel="noreferrer" style={{ color: C.accent }}>/{r.shortCode}</a>
              <span className="badge off">{r.reason.toLowerCase()}</span>
              {r.linkDisabled && <span className="tag-off">disabled</span>}
              <span style={{ color: C.muted, marginLeft: 'auto', fontSize: 11 }}>{new Date(r.createdAt).toLocaleString()}</span>
            </div>
            <div style={{ fontFamily: C.mono, fontSize: 12, color: C.muted, overflowWrap: 'anywhere' }}>
              → {r.destination ?? 'link no longer exists'}
            </div>
            {r.details && <p style={{ fontFamily: C.space, fontSize: 14, color: C.text, lineHeight: 1.6 }}>{r.details}</p>}
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {r.status === 'OPEN' && r.destination && (
                <button className="btn-danger" disabled={busy === r.id} onClick={() => act(r, 'disable')}>Disable link</button>
              )}
              {r.status === 'OPEN' && (
                <button className="btn-g btn-sm" disabled={busy === r.id} onClick={() => act(r, 'dismiss')}>Dismiss</button>
              )}
              {r.linkDisabled && (
                <button className="btn-g btn-sm" disabled={busy === r.id} onClick={() => act(r, 'enable')}>Re-enable link</button>
              )}
            </div>
          </article>
        ))}
      </main>
    </div>
  );
}
