import { useState } from 'react';
import { updateLink } from '../services/api';
import { Modal } from './Modal';

/** Edit a link's destination and/or alias. */
export function EditModal({ entry, onClose, onSave, toast }) {
  const [originalUrl, setOriginalUrl] = useState(entry.originalUrl || '');
  const [alias, setAlias] = useState(entry.code || '');
  const [saving, setSaving] = useState(false);

  async function save(e) {
    e.preventDefault();
    const url = originalUrl.trim();
    const newAlias = alias.trim();
    if (!url) { toast('URL is required.', 'error'); return; }

    const changes = {};
    if (url !== entry.originalUrl) changes.originalUrl = url;
    if (newAlias && newAlias !== entry.code) changes.customAlias = newAlias;
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
      <form onSubmit={save} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
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
        <div className="modal-actions">
          <button type="button" className="btn-g btn-sm" onClick={onClose} disabled={saving}>Cancel</button>
          <button type="submit" className="btn-p" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button>
        </div>
      </form>
    </Modal>
  );
}
