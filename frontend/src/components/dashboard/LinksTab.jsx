import { C } from '../../constants/theme';
import { LinkRow } from '../LinkRow';
import { StatCard } from './StatCard';

const muted = { padding: '40px 16px', textAlign: 'center', color: C.muted, fontFamily: C.mono, fontSize: 13 };

export function LinksTab({ links, toast, onCreate }) {
  const { items, totalItems, summary, query, setQuery, loading, loadingMore, error, hasMore, reload, loadMore } = links;

  let body;
  if (loading) {
    body = <div style={muted}>Loading…</div>;
  } else if (error && items.length === 0) {
    body = (
      <div role="alert" style={{ ...muted, display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'center', color: C.error }}>
        {error}
        <button className="btn-g btn-sm" onClick={reload}>Retry</button>
      </div>
    );
  } else if (items.length === 0) {
    body = (
      <div style={muted}>
        {query ? 'No links match your search.' : (
          <>No links yet. <button className="btn-p btn-sm" style={{ marginLeft: 8 }} onClick={onCreate}>+ New link</button></>
        )}
      </div>
    );
  } else {
    body = items.map(entry => <LinkRow key={entry.code} entry={entry} onRefresh={reload} toast={toast} />);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24, animation: 'fadeUp .3s both' }}>
      <div className="stat-grid">
        <StatCard label="Total links" value={summary?.totalLinks} icon="✦" variant="p" />
        <StatCard label="Total clicks" value={summary?.totalClicks} icon="◉" variant="pk" />
      </div>

      <div className="gcard" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="toolbar">
          <label htmlFor="link-search" className="sr-only">Search links</label>
          <input id="link-search" className="inp" type="search" value={query} onChange={e => setQuery(e.target.value)}
            placeholder="Search alias or destination…" />
          <span style={{ fontFamily: C.mono, fontSize: 12, color: C.muted, marginLeft: 'auto' }}>
            {loading ? '' : `${totalItems} link${totalItems !== 1 ? 's' : ''}`}
          </span>
        </div>
        <div className="lrow lrow-head" aria-hidden="true">
          <span>Alias</span><span>Destination</span><span>Clicks</span><span style={{ textAlign: 'right' }}>Actions</span>
        </div>
        {body}
        {!loading && error && items.length > 0 && (
          <div role="alert" style={{ ...muted, padding: 16, color: C.error }}>{error}</div>
        )}
        {!loading && hasMore && (
          <div style={{ padding: 16, display: 'flex', justifyContent: 'center' }}>
            <button className="btn-g btn-sm" onClick={loadMore} disabled={loadingMore}>
              {loadingMore ? 'Loading…' : `Load more (${items.length} of ${totalItems})`}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
