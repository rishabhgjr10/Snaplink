import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { C } from '../constants/theme';
import { Logo } from '../components/Logo';
import { Footer } from '../layouts/MainLayout';
import { extractShortCode, reportLink } from '../services/api';
import { MaintenanceBanner } from '../components/MaintenanceBanner';

const REASONS = [
  ['PHISHING', 'Phishing or fake login page'],
  ['MALWARE', 'Malware or harmful download'],
  ['SPAM', 'Spam'],
  ['ILLEGAL', 'Illegal content'],
  ['OTHER', 'Something else'],
];

/** Public abuse report form. */
export function ReportPage() {
  const [params] = useSearchParams();
  const [link, setLink] = useState(params.get('code') || '');
  const [reason, setReason] = useState('PHISHING');
  const [details, setDetails] = useState('');
  const [state, setState] = useState({ sending: false, done: false, error: '' });

  async function submit(e) {
    e.preventDefault();
    const code = extractShortCode(link);
    if (!code) { setState({ sending: false, done: false, error: 'Enter the short link or its code.' }); return; }
    setState({ sending: true, done: false, error: '' });
    try {
      await reportLink(code, reason, details.trim());
      setState({ sending: false, done: true, error: '' });
    } catch (err) {
      setState({
        sending: false, done: false,
        error: err.status === 404 ? 'We could not find that short link.' : (err.message || 'Could not send the report.'),
      });
    }
  }

  return (
    <div style={{ minHeight: '100vh', color: C.text, display: 'flex', flexDirection: 'column' }}>
      <nav style={{ padding: '0 16px', height: 64, display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: `1px solid ${C.border}` }}>
        <Link to="/" style={{ textDecoration: 'none' }}><Logo size="sm" /></Link>
        <Link to="/" style={{ fontFamily: C.mono, fontSize: 13, color: C.muted, textDecoration: 'none' }}>← back</Link>
      </nav>
      <main style={{ flex: 1, maxWidth: 560, margin: '0 auto', padding: '56px 16px 80px', width: '100%' }}>
        <MaintenanceBanner compact style={{ marginBottom: 28 }} />
        <h1 style={{ fontFamily: C.display, fontWeight: 800, fontSize: 34, marginBottom: 10 }}>Report a link</h1>
        <p style={{ fontFamily: C.mono, fontSize: 12.5, color: C.muted, lineHeight: 1.7, marginBottom: 28 }}>
          Tell us about a SnapLink short link that leads to phishing, malware, spam or illegal content. We review every report.
        </p>
        {state.done ? (
          <div role="status" className="gcard" style={{ padding: 20, fontFamily: C.mono, fontSize: 13, lineHeight: 1.6 }}>
            Thank you. Your report was received and will be reviewed.
          </div>
        ) : (
          <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div>
              <label className="field-label" htmlFor="report-link">Short link or code *</label>
              <input id="report-link" className="inp" required value={link} onChange={e => setLink(e.target.value)}
                placeholder="https://bauna.xyz/abc1234" />
            </div>
            <div>
              <label className="field-label" htmlFor="report-reason">Reason *</label>
              <select id="report-reason" className="inp" value={reason} onChange={e => setReason(e.target.value)}>
                {REASONS.map(([value, text]) => <option key={value} value={value}>{text}</option>)}
              </select>
            </div>
            <div>
              <label className="field-label" htmlFor="report-details">Details</label>
              <textarea id="report-details" className="inp" rows={4} maxLength={1000} value={details}
                onChange={e => setDetails(e.target.value)} placeholder="Optional: what did you see?" />
            </div>
            {state.error && <p role="alert" style={{ fontFamily: C.mono, fontSize: 12, color: C.error }}>{state.error}</p>}
            <button type="submit" className="btn-p" disabled={state.sending} style={{ justifyContent: 'center' }}>
              {state.sending ? 'Sending…' : 'Send report'}
            </button>
          </form>
        )}
      </main>
      <Footer />
    </div>
  );
}
