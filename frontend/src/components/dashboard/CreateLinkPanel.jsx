import { useEffect, useState } from 'react';
import { C } from '../../constants/theme';
import { shortenUrl } from '../../services/api';
import { useBackendStatus, waitForBackend } from '../../hooks/useBackendStatus';
import { BackendStatusBanner } from '../BackendStatus';
import { QRButton } from '../QRButton';

/**
 * Slide-in form for a new link. Only fields the backend actually stores are offered:
 * destination and optional alias.
 */
export function CreateLinkPanel({ toast, onCreated, onClose }) {
  const [url, setUrl] = useState('');
  const [alias, setAlias] = useState('');
  const [shortening, setShortening] = useState(false);
  const [result, setResult] = useState(null);
  const backend = useBackendStatus();

  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  async function handleShorten(e) {
    e.preventDefault();
    if (shortening) return; // ignore double submits
    if (!url.trim()) { toast('Enter a URL.', 'error'); return; }
    setShortening(true);
    try {
      // Render may still be cold-starting: wait for the health check instead of letting the request hang.
      if (backend.status !== 'up' && !(await waitForBackend())) {
        toast('Server is not responding. Please retry in a moment.', 'error');
        return;
      }
      const created = await shortenUrl({ url: url.trim(), alias: alias.trim() || undefined });
      setResult(created);
      toast('Link created!', 'success');
      setUrl('');
      setAlias('');
      onCreated();
    } catch (err) {
      if (err.unavailable) toast(err.message, 'error');
      else if (err.status === 409) toast('That alias is already taken. Try another.', 'error');
      else if (err.status === 429) toast('Rate limit reached. Please wait and try again.', 'error');
      else toast(err.message || 'Could not shorten.', 'error');
    } finally {
      setShortening(false);
    }
  }

  function copyResult() {
    navigator.clipboard.writeText(result.shortUrl)
      .then(() => toast('Copied!', 'success'))
      .catch(() => toast('Copy failed.', 'error'));
  }

  return (
    <>
      <div className="overlay" onClick={onClose} />
      <div className="panel" role="dialog" aria-modal="true" aria-labelledby="create-title">
        <div style={{ padding: '24px 28px 20px', borderBottom: '1px solid rgba(255,255,255,0.07)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <h2 id="create-title" style={{ fontFamily: C.display, fontWeight: 800, fontSize: 20, color: C.text }}>New short link</h2>
          <button className="bico" onClick={onClose} aria-label="Close">✕</button>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', padding: '24px 28px', display: 'flex', flexDirection: 'column', gap: 18 }}>
          {!result && <BackendStatusBanner />}
          {result ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16, alignItems: 'center', paddingTop: 40, textAlign: 'center', animation: 'fadeUp .3s both' }}>
              <div aria-hidden="true" style={{ fontSize: 48 }}>✦</div>
              <h3 style={{ fontFamily: C.display, fontWeight: 800, fontSize: 22, color: C.text }}>Link created!</h3>
              <a href={result.shortUrl} target="_blank" rel="noreferrer"
                style={{ fontFamily: C.mono, fontSize: 18, color: C.accent, fontWeight: 600, textDecoration: 'none', overflowWrap: 'anywhere' }}>
                {result.shortUrl}
              </a>
              <div style={{ display: 'flex', gap: 10 }}>
                <QRButton url={result.shortUrl} />
                <button className="btn-p" onClick={copyResult}>Copy link</button>
              </div>
              <button className="btn-g" onClick={() => setResult(null)} style={{ marginTop: 8 }}>Create another</button>
            </div>
          ) : (
            <form onSubmit={handleShorten} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div>
                <label className="field-label" htmlFor="new-url">Destination URL *</label>
                <input id="new-url" className="inp" type="url" required value={url} onChange={e => setUrl(e.target.value)}
                  placeholder="https://example.com/very-long-url" autoFocus />
              </div>
              <div>
                <label className="field-label" htmlFor="new-alias">Custom alias</label>
                <input id="new-alias" className="inp" value={alias} onChange={e => setAlias(e.target.value)}
                  placeholder="my-alias (optional)" pattern="[A-Za-z0-9_\-]{3,32}"
                  title="3-32 letters, numbers, '-' or '_'" />
              </div>
              <button type="submit" className="btn-p" disabled={shortening} style={{ marginTop: 4, justifyContent: 'center' }}>
                {shortening
                  ? (backend.status === 'up' ? 'Shortening…' : 'Waiting for server…')
                  : 'Create short link ✦'}
              </button>
            </form>
          )}
        </div>
      </div>
    </>
  );
}
