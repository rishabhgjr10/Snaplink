import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { C } from '../constants/theme';
import { Logo } from '../components/Logo';
import { useAuth } from '../context/AuthContext';

export function NavBar({ toast }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const fn = () => setScrolled(window.scrollY > 40);
    window.addEventListener('scroll', fn);
    return () => window.removeEventListener('scroll', fn);
  }, []);

  async function handleLogout() {
    await logout();
    navigate('/');
  }

  return (
    <nav className="nav-wrap" style={{
      position: 'fixed', top: 0, left: 0, right: 0, zIndex: 200,
      height: 66, display: 'flex', alignItems: 'center', padding: '0 32px',
      justifyContent: 'space-between', transition: 'background .35s, border-color .35s',
      ...(scrolled ? {
        background: 'rgba(7,6,15,0.85)',
        backdropFilter: 'blur(24px)',
        WebkitBackdropFilter: 'blur(24px)',
        borderBottom: '1px solid rgba(255,255,255,0.07)',
      } : {}),
    }}>
      {/* ── Brand: SnapLink by bauna.xyz ── */}
      <Link to={user ? '/dashboard' : '/'} style={{ textDecoration: 'none' }}>
        <Logo size="sm" withBadge />
      </Link>

      {/* ── Auth Actions (Features/Analytics/Pricing removed) ── */}
      <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
        {user?.role === 'admin' && (
          <Link to="/v1/admin" className="btn-g btn-sm" style={{ fontFamily: C.mono, fontSize: 11, color: C.accent }}>admin</Link>
        )}
        {user ? (
          <>
            <span
              className="nav-user-email"
              style={{
                fontFamily: C.mono, fontSize: 12, color: C.muted,
                maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                display: 'inline-block',
              }}
              title={user.email}
            >
              {user.email}
            </span>
            <Link to="/dashboard" className="btn-p btn-sm" style={{ textDecoration: 'none', padding: '7px 16px' }}>
              Dashboard →
            </Link>
            <button onClick={handleLogout} className="btn-g btn-sm" style={{ padding: '7px 14px' }}>
              Logout
            </button>
          </>
        ) : (
          <>
            <Link to="/login" className="btn-g btn-sm">Sign in</Link>
            <Link to="/signup" className="btn-p btn-sm">Get started free →</Link>
          </>
        )}
      </div>
    </nav>
  );
}

export function Footer() {
  return (
    <footer style={{ borderTop: '1px solid rgba(255,255,255,0.06)', padding: '36px 32px', background: 'rgba(7,6,15,0.95)' }}>
      <div style={{ maxWidth: 1100, margin: '0 auto', display: 'flex', gap: 24, alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap' }}>
        <Logo size="sm" />
        <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap', alignItems: 'center' }}>
          {[['Privacy Policy', '/privacy'], ['Terms', '/terms'], ['GitHub ↗', 'https://github.com/rishabhgjr10/Snaplink']].map(([label, href]) => (
            <a key={label} href={href} target={href.startsWith('http') ? '_blank' : undefined} rel="noreferrer"
              style={{ fontFamily: C.mono, fontSize: 12, color: C.muted, textDecoration: 'none', transition: 'color .15s' }}
              onMouseEnter={e => e.currentTarget.style.color = C.text}
              onMouseLeave={e => e.currentTarget.style.color = C.muted}>{label}</a>
          ))}
        </div>
        <p style={{ fontFamily: C.mono, fontSize: 11, color: C.muted2, margin: 0 }}>
          © {new Date().getFullYear()} SnapLink by bauna.xyz · All rights reserved.
        </p>
      </div>
    </footer>
  );
}
