import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { C } from '../../constants/theme';
import { getMe } from '../../services/api';

export function SettingsTab({ user }) {
  const [admin, setAdmin] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getMe().then(me => !cancelled && setAdmin(me.admin)).catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const row = (label, value) => (
    <>
      <div style={{ fontFamily: C.mono, fontSize: 13, color: C.muted, marginBottom: 6 }}>{label}</div>
      <div style={{ fontFamily: C.mono, fontSize: 14, color: C.text, marginBottom: 20, overflowWrap: 'anywhere' }}>{value}</div>
    </>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, animation: 'fadeUp .3s both', maxWidth: 580 }}>
      <div className="gcard" style={{ padding: '24px 26px' }}>
        <h2 style={{ fontFamily: C.display, fontWeight: 800, fontSize: 18, color: C.text, marginBottom: 18 }}>Account</h2>
        {row('Email', user?.email || '—')}
        {row('Role', admin ? 'Admin' : 'User')}
        {admin && <Link to="/admin" className="btn-g btn-sm">Open moderation</Link>}
      </div>
      <div className="gcard" style={{ padding: '24px 26px' }}>
        <h2 style={{ fontFamily: C.display, fontWeight: 800, fontSize: 18, color: C.text, marginBottom: 12 }}>Your data</h2>
        <p style={{ fontFamily: C.mono, fontSize: 12, color: C.muted, lineHeight: 1.7 }}>
          To delete your account and all links you created, follow the steps in the <Link to="/privacy" style={{ color: C.accent }}>Privacy Policy</Link>.
        </p>
      </div>
    </div>
  );
}
