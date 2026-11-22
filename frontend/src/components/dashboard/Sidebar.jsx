import { Link } from 'react-router-dom';
import { C } from '../../constants/theme';
import { Logo } from '../Logo';

export const TABS = [
  { id: 'links', icon: '✦', label: 'Short Links' },
  { id: 'analytics', icon: '◉', label: 'Analytics' },
  { id: 'qr', icon: '▦', label: 'QR Tools' },
  { id: 'settings', icon: '◈', label: 'Settings' },
];

/** Fixed on desktop; an off-canvas drawer below 768px (see .dash-sidebar in global.css). */
export function Sidebar({ activeTab, onSelect, open, onClose, user, onLogout }) {
  return (
    <>
      <div className={`dash-scrim${open ? ' open' : ''}`} onClick={onClose} aria-hidden="true" />
      <aside className={`dash-sidebar${open ? ' open' : ''}`} aria-label="Dashboard navigation">
        <div aria-hidden="true" style={{ position: 'absolute', right: 0, top: '20%', bottom: '20%', width: 1, background: 'linear-gradient(180deg, transparent, rgba(108,99,255,0.35), transparent)' }} />

        <Link to="/dashboard" style={{ textDecoration: 'none' }} onClick={onClose}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, padding: '20px 24px', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
            <Logo size="sm" />
            <span style={{
              fontFamily: C.mono, fontSize: 11, color: 'rgba(192, 132, 252, 0.9)', background: 'rgba(168, 85, 247, 0.1)',
              border: '1px solid rgba(168, 85, 247, 0.22)', padding: '2px 8px', borderRadius: 9999, width: 'fit-content',
            }}>by bauna.xyz</span>
          </div>
        </Link>

        <nav style={{ flex: 1, paddingTop: 14, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {TABS.map(t => (
            <button key={t.id} className={`nv${activeTab === t.id ? ' active' : ''}`}
              aria-current={activeTab === t.id ? 'page' : undefined}
              onClick={() => { onSelect(t.id); onClose(); }}>
              <span aria-hidden="true" style={{ fontSize: 15 }}>{t.icon}</span>
              <span>{t.label}</span>
              {activeTab === t.id && <span className="nv-dot" />}
            </button>
          ))}
        </nav>

        <div style={{ padding: '16px 20px 24px', borderTop: '1px solid rgba(255,255,255,0.06)' }}>
          <div title={user?.email} style={{ fontFamily: C.mono, fontSize: 11, color: C.muted, marginBottom: 8, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {user?.email}
          </div>
          <button className="btn-g btn-sm" style={{ width: '100%', justifyContent: 'center' }} onClick={onLogout}>Logout</button>
        </div>
      </aside>
    </>
  );
}
