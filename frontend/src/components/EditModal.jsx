import { useState } from 'react';
import { updateLink } from '../services/api';
import { choiceFromIso, describeLink, expiryToIso, isoToLocalInput, localInputToIso } from '../lib/lifetime';
import { ExpiryPicker } from './ExpiryPicker';
import { LinkLimitsFields } from './LinkLimitsFields';
import { Modal } from './Modal';

/** Edit a link's destination, alias, expiry, start time and click limit. */
export function EditModal({ entry, onClose, onSave, toast }) {
  const [originalUrl, setOriginalUrl] = useState(entry.originalUrl || '');
  const [alias, setAlias] = useState(entry.code || '');
  const initialExpiry = choiceFromIso(entry.expiresAt);
  const [expiry, setExpiry] = useState(initialExpiry);
  const initialStart = isoToLocalInput(entry.startsAt);
  const [startsAt, setStartsAt] = useState(initialStart);
  const initialMax = entry.maxClicks ? String(entry.maxClicks) : '';
  const [maxClicks, setMaxClicks] = useState(initialMax);
  const [saving, setSaving] = useState(false);
  const state = describeLink(entry);
  const stopped = entry.status === 'EXPIRED' || entry.status === 'LIMIT_REACHED';

  async function save(e) {
    e.preventDefault();
    const url = originalUrl.trim();
    const newAlias = alias.trim();
    if (!url) { toast('URL is required.', 'error'); return; }
    if (expiry.preset === 'custom' && !expiry.custom) { toast('Pick a custom expiry date and time.', 'error'); return; }

    const changes = {};
    if (url !== entry.originalUrl) changes.originalUrl = url;
    if (newAlias && newAlias !== entry.code) changes.customAlias = newAlias;

    // Only send lifetime fields the user actually changed.
    const expiryChanged = expiry.preset !== initialExpiry.preset || expiry.custom !== initialExpiry.custom;
    if (expiryChanged) {
      const iso = expiryToIso(expiry);
      if (iso) changes.expiresAt = iso; else changes.clearExpiresAt = true;
    }
    if (startsAt !== initialStart) {
      const iso = localInputToIso(startsAt);
      if (iso) changes.startsAt = iso; else changes.clearStartsAt = true;
    }
    if (maxClicks !== initialMax) {
      if (maxClicks) changes.maxClicks = Number(maxClicks); else changes.clearMaxClicks = true;
    }
    if (!Object.keys(changes).length) { onClose(); return; }

    setSaving(true);
    try {
      await updateLink(entry.code, changes);
      toast('Link updated.', 'success');
      onSave();
      onClose();
    } catch (err) {
      toast(err.message || 'Save failed.', 'error');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={`Edit /${entry.code}`} onClose={onClose}>
      <form onSubmit={save} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {stopped && (
          <p role="note" style={{ color: '#fbbf24', fontSize: 12 }}>
            {entry.status === 'EXPIRED'
              ? 'This link has expired. Choose a new expiry (or Never) to reactivate it.'
              : 'This link reached its click limit. Raise or clear the limit to reactivate it.'}
          </p>
        )}
        <div>
          <label className="field-label" htmlFor="edit-url">Destination URL</label>
          <input id="edit-url" className="inp" type="url" value={originalUrl}
            onChange={e => setOriginalUrl(e.target.value)} required />
        </div>
        <div>
          <label className="field-label" htmlFor="edit-alias">Alias</label>
          <input id="edit-alias" className="inp" value={alias} onChange={e => setAlias(e.target.value)}
            pattern="[A-Za-z0-9_\-]{3,32}" title="3-32 letters, numbers, '-' or '_'" />
          <p style={{ marginTop: 6, fontSize: 11 }}>
            Changing the alias breaks the old short link. Click history is kept.
          </p>
        </div>
        <div>
          <ExpiryPicker value={expiry} onChange={setExpiry} label="Expiry" />
          <p style={{ marginTop: 6, fontSize: 11 }}>Currently: {state.label}{state.detail ? ` (${state.detail})` : ''}</p>
        </div>
        <LinkLimitsFields startsAt={startsAt} onStartsAt={setStartsAt} maxClicks={maxClicks} onMaxClicks={setMaxClicks} />
        <div className="modal-actions">
          <button type="button" className="btn-g btn-sm" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" className="btn-p" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button>
        </div>
      </form>
    </Modal>
  );
}
