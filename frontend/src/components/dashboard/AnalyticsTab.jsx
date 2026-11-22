import { useEffect, useState } from 'react';
import { C } from '../../constants/theme';
import { AnalyticsPanel } from '../AnalyticsPanel';

/** Pick any loaded link and see its analytics (no silent "first 10 only" cut-off). */
export function AnalyticsTab({ links }) {
  const { items, totalItems, hasMore, loadMore, loadingMore, loading } = links;
  const [selected, setSelected] = useState('');

  useEffect(() => {
    if (!selected && items.length) setSelected(items[0].code);
    if (selected && items.length && !items.some(i => i.code === selected)) setSelected(items[0].code);
  }, [items, selected]);

  if (loading) return <p style={{ fontFamily: C.mono, fontSize: 13, color: C.muted }}>Loading…</p>;
  if (!items.length) {
    return <p style={{ fontFamily: C.mono, fontSize: 13, color: C.muted }}>Create a link to see its analytics here.</p>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, animation: 'fadeUp .3s both' }}>
      <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
        <label className="field-label" htmlFor="analytics-link" style={{ margin: 0 }}>Link</label>
        <select id="analytics-link" className="inp" style={{ maxWidth: 360 }} value={selected} onChange={e => setSelected(e.target.value)}>
          {items.map(l => <option key={l.code} value={l.code}>/{l.code} — {l.originalUrl}</option>)}
        </select>
        {hasMore && (
          <button className="btn-g btn-sm" onClick={loadMore} disabled={loadingMore}>
            {loadingMore ? 'Loading…' : `Load more links (${items.length} of ${totalItems})`}
          </button>
        )}
      </div>
      {selected && (
        <div className="gcard" style={{ padding: '18px 20px' }}>
          <div style={{ fontFamily: C.mono, fontSize: 13, color: C.accent, fontWeight: 600 }}>/{selected}</div>
          <AnalyticsPanel key={selected} code={selected} />
        </div>
      )}
    </div>
  );
}
