import { useState } from 'react';
import { deleteLink } from '../services/api';
import { describeLink, formatDateTime } from '../lib/lifetime';
import { QRButton } from './QRButton';
import { AnalyticsPanel } from './AnalyticsPanel';
import { EditModal } from './EditModal';
import { ConfirmDialog } from './Modal';

const icon = { width: 14, height: 14, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 2, 'aria-hidden': true };

export function LinkRow({ entry, now = Date.now(), onRefresh, toast }) {
  const [expanded, setExpanded] = useState(false);
  const [editing, setEditing] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const state = describeLink(entry, now);
  const inactive = state.tone === 'off' || entry.status === 'SCHEDULED';

  async function handleDelete() {
    setDeleting(true);
    try {
      await deleteLink(entry.code);
      toast('Link deleted.', 'success');
      setConfirmDelete(false);
      onRefresh();
    } catch (err) {
      toast(err.message || 'Failed to delete.', 'error');
    } finally {
      setDeleting(false);
    }
  }

  function copyLink() {
    navigator.clipboard.writeText(entry.shortUrl)
      .then(() => toast('Copied!', 'success'))
      .catch(() => toast('Copy failed.', 'error'));
  }

  const tooltip = [
    entry.startsAt && `Starts ${formatDateTime(entry.startsAt)}`,
    entry.expiresAt ? `Expires ${formatDateTime(entry.expiresAt)}` : 'Never expires',
    entry.maxClicks && `Limit: ${entry.maxClicks.toLocaleString()} clicks`,
  ].filter(Boolean).join(' · ');

  return (
    <>
      {editing && <EditModal entry={entry} onClose={() => setEditing(false)} onSave={onRefresh} toast={toast} />}
      {confirmDelete && (
        <ConfirmDialog
          title={`Delete /${entry.code}?`}
          message="The short link stops working and its click history is deleted. This cannot be undone."
          confirmLabel="Delete link" danger busy={deleting}
          onConfirm={handleDelete} onCancel={() => setConfirmDelete(false)} />
      )}
      <div className={`lrow${inactive ? ' lrow-inactive' : ''}`}>
        <a className="lrow-code" href={entry.shortUrl} target="_blank" rel="noreferrer" title={entry.shortUrl}>
          /{entry.code}
        </a>
        <div className="lrow-dest" title={entry.originalUrl}>{entry.originalUrl}</div>
        <div className="lrow-status" title={tooltip}>
          <span className={`pill pill-${state.tone}`}>{state.label}</span>
          {state.detail && <span className="lrow-status-detail">{state.detail}</span>}
        </div>
        <div className="lrow-clicks">{entry.clicks.toLocaleString()}<span className="lrow-label"> clicks</span></div>
        <div className="lrow-actions">
          <QRButton url={entry.shortUrl} expiresAt={entry.expiresAt} maxClicks={entry.maxClicks} />
          <button className="bico" onClick={copyLink} aria-label={`Copy /${entry.code}`} title="Copy link">
            <svg {...icon}><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
          </button>
          <button className={`bico${expanded ? ' pk' : ''}`} onClick={() => setExpanded(e => !e)}
            aria-expanded={expanded} aria-label={`${expanded ? 'Hide' : 'Show'} analytics for /${entry.code}`} title="Analytics">
            <svg {...icon}><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
          </button>
          <button className="bico" onClick={() => setEditing(true)} aria-label={`Edit /${entry.code}`} title="Edit link">
            <svg {...icon}><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button className="bico red" onClick={() => setConfirmDelete(true)} aria-label={`Delete /${entry.code}`} title="Delete link">
            <svg {...icon}><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/></svg>
          </button>
        </div>
      </div>
      {expanded && (
        <div className="lrow-detail" style={{ animation: 'expandD .22s cubic-bezier(.22,1,.36,1) both' }}>
          <AnalyticsPanel code={entry.code} />
        </div>
      )}
    </>
  );
}
