import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { C, primaryBtn } from '../constants/theme';
import { useAuth } from '../context/AuthContext';
import { AuthCard, InputField } from '../components/AuthCard';

export function LoginPage({ toast }) {
  const { loginWithEmail, loginWithGoogle, loginWithGithub } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/dashboard';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    if (!email || !password) { setError('Please fill in all fields.'); return; }
    setLoading(true); setError('');
    try {
      await loginWithEmail(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message || 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  }

  async function handleOAuth(provider) {
    setError(''); setLoading(true);
    try {
      if (provider === 'google') await loginWithGoogle();
      else await loginWithGithub();
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message || `${provider} login failed.`);
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthCard title="Welcome back.">
      {/* OAuth buttons */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 20 }}>
        <button type="button" onClick={() => handleOAuth('google')} disabled={loading}
          style={{ ...oauthBtn, background: 'rgba(255,255,255,0.06)' }}>
          <svg width="18" height="18" viewBox="0 0 48 48"><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/><path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/><path fill="#FBBC05" d="M10.53 28.59a14.5 14.5 0 0 1 0-9.18l-7.98-6.19a24.01 24.01 0 0 0 0 21.56l7.98-6.19z"/><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/></svg>
          Continue with Google
        </button>
        <button type="button" onClick={() => handleOAuth('github')} disabled={loading}
          style={{ ...oauthBtn, background: 'rgba(255,255,255,0.06)' }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/></svg>
          Continue with GitHub
        </button>
      </div>

      {/* Divider */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '4px 0 16px' }}>
        <div style={{ flex: 1, height: 1, background: 'rgba(255,255,255,0.08)' }} />
        <span style={{ fontFamily: C.mono, fontSize: 11, color: C.muted, textTransform: 'uppercase', letterSpacing: '.06em' }}>or</span>
        <div style={{ flex: 1, height: 1, background: 'rgba(255,255,255,0.08)' }} />
      </div>

      {/* Email / Password */}
      <form onSubmit={handleSubmit}>
        <InputField label="Email" type="email" value={email} onChange={e => setEmail(e.target.value)} autoComplete="email" />
        <InputField label="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} autoComplete="current-password" />
        {error && <div style={{ color: C.error, fontFamily: C.mono, fontSize: 13, marginBottom: 14 }}>{error}</div>}
        <button type="submit" disabled={loading} style={primaryBtn}
          onMouseEnter={e => { e.currentTarget.style.opacity = '.88'; e.currentTarget.style.boxShadow = '0 0 20px rgba(108,99,255,0.35)'; }}
          onMouseLeave={e => { e.currentTarget.style.opacity = '1'; e.currentTarget.style.boxShadow = 'none'; }}
        >{loading ? 'Logging in…' : 'Login'}</button>
      </form>
      <p style={{ fontFamily: C.mono, fontSize: 12, color: C.muted, marginTop: 18, textAlign: 'center' }}>
        No account? <Link to="/signup" style={{ color: C.accent }}>Sign up</Link>
      </p>
    </AuthCard>
  );
}

/* ── Shared OAuth button style ─────────────────────────────────────────────── */
const oauthBtn = {
  display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
  width: '100%', padding: '10px 0', borderRadius: 9,
  border: '1px solid rgba(255,255,255,0.10)', color: '#ccc',
  fontFamily: 'var(--font-mono, monospace)', fontSize: 13, cursor: 'pointer',
  transition: 'background .15s, border-color .15s',
};
