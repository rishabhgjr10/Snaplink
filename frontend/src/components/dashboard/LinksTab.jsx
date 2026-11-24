import { useState } from 'react';
import { C } from '../../constants/theme';
import { deleteExpiredLinks } from '../../services/api';
import { useNow } from '../../lib/lifetime';
import { LinkRow } from '../LinkRow';
import { ConfirmDialog } from '../Modal';
import { StatCard } from './StatCard';

const muted = { padding: '40px 16px', textAlign: 'center', color: C.muted, fontFamily: C.mono, fontSize: 13 };

const FILTERS = [
  { id: 'all', label: 'All' },
  { id: 'active', label: 'Active' },
  { id: 'scheduled', label: 'Scheduled' },
  { id: 'expired', label: 'Expired' },
];

const EMPTY = {
  all: 'No links yet.',
  active: 'No active links.',
  scheduled: 'No scheduled links. Set a start time under "Advanced" when creating a link.',
  expired: 'No expired links.',
};

export function LinksTab({ links, toast, onCreate }) {
  const { items, totalItems, summary, query, setQuery, status, setStatus, sort, setSort,
    loading, loadingMore, error, hasMore, reload, loadMore } = links;
  const now = useNow(15000); // one shared ticker keeps every countdown current
  const [confirmBulk, setConfirmBulk] = useState(false);
  const [deletingExpired, setDeletingExpired] = useState(false);

  async function handleDeleteExpired() {
    setDeletingExpired(true);
    try {
      const n = await deleteExpiredLinks();
      toast(`Deleted ${n} expired link${n === 1 ? '' : 's'}.`, 'success');
      setConfirmBulk(false);
      reload();
    } catch (err) {
      toast(err.message || 'Could not delete expired links.', 'error');
    } finally {
      setDeletingExpired(false);
    }
  }

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
        {query ? 'No links match your search.' : EMPTY[status]}
        {!query && status === 'all' && (
          <button className="btn-p btn-sm" style={{ marginLeft: 8 }} onClick={onCreate}>+ New link</button>
        )}
      </div>
    );
  } else {
    body = items.map(entry => <LinkRow key={entry.code} entry={entry} now={now} onRefresh={reload} toast={toast} />);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24, animation: 'fadeUp .3s both' }}>
      {confirmBulk && (
        <ConfirmDialog
          title="Delete all expired links?"
          message={`This deletes ${summary?.expiredLinks ?? 'all'} expired link(s), including links that reached their click limit, together with their click history. This cannot be undone.`}
          confirmLabel="Delete expired links" danger busy={deletingExpired}
          onConfirm={handleDeleteExpired} onCancel={() => setConfirmBulk(false)} />
      )}

      <div className="stat-grid">
        <StatCard label="Total links" value={summary?.totalLinks} icon="✦" variant="p" />
        <StatCard label="Total clicks" value={summary?.totalClicks} icon="◉" variant="pk" />
        <StatCard label="Active" value={summary?.activeLinks} icon="●" variant="lm" />
        <StatCard label="Expired" value={summary?.expiredLinks} icon="⌛" variant="am" />
      </div>

      <div className="gcard" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="toolbar">
          <label htmlFor="link-search" className="sr-only">Search links</label>
          <input id="link-search" className="inp" type="search" value={query} onChange={e => setQuery(e.target.value)}
            placeholder="Search alias or destination…" />
          <div role="tablist" aria-label="Filter links by status" className="seg">
            {FILTERS.map(f => (
              <button key={f.id} role="tab" aria-selected={status === f.id}
                className={`seg-btn${status === f.id ? ' on' : ''}`} onClick={() => setStatus(f.id)}>
                {f.label}
              </button>
            ))}
          </div>
          <label htmlFor="link-sort" className="sr-only">Sort links</label>
          <select id="link-sort" className="inp" style={{ width: 'auto' }} value={sort} onChange={e => setSort(e.target.value)}>
            <option value="newest">Newest first</option>
            <option value="expiring">Expiring soonest</option>
          </select>
          <span style={{ fontFamily: C.mono, fontSize: 12, color: C.muted, marginLeft: 'auto' }}>
            {loading ? '' : `${totalItems} link${totalItems !== 1 ? 's' : ''}`}
          </span>
        </div>

        {status === 'expired' && !loading && items.length > 0 && (
          <div style={{ padding: '10px 18px', borderBottom: '1px solid rgba(255,255,255,0.05)', display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
            <span style={{ fontFamily: C.mono, fontSize: 12, color: C.muted, flex: 1, minWidth: 200 }}>
              Expired links no longer redirect. Edit one to give it a new expiry, or clean them up.
            </span>
            <button className="btn-danger" onClick={() => setConfirmBulk(true)}>Delete all expired</button>
          </div>
        )}

        <div className="lrow lrow-head" aria-hidden="true">
          <span>Alias</span><span>Destination</span><span>Status</span><span>Clicks</span><span style={{ textAlign: 'right' }}>Actions</span>
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
