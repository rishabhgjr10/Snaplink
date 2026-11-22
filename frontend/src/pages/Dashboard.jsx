import { lazy, Suspense, useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { C } from '../constants/theme';
import { useAuth } from '../context/AuthContext';
import { useLinks } from '../hooks/useLinks';
import { useBackendStatus, wakeBackend } from '../hooks/useBackendStatus';
import { BackendStatusBanner, BackendStatusPill } from '../components/BackendStatus';
import { Sidebar, TABS } from '../components/dashboard/Sidebar';
import { CreateLinkPanel } from '../components/dashboard/CreateLinkPanel';
import { LinksTab } from '../components/dashboard/LinksTab';
import { AnalyticsTab } from '../components/dashboard/AnalyticsTab';
import { SettingsTab } from '../components/dashboard/SettingsTab';

// QR tooling (qr-code-styling, jsQR, JSZip) loads only when the tab is opened.
const QrToolsTab = lazy(() => import('../components/dashboard/QrToolsTab').then(m => ({ default: m.QrToolsTab })));

function AmbientBg() {
  return (
    <div aria-hidden="true" style={{ position: 'fixed', inset: 0, pointerEvents: 'none', zIndex: 0, overflow: 'hidden' }}>
      <div style={{ position: 'absolute', width: 600, height: 600, borderRadius: '50%', top: -200, left: -100, background: 'radial-gradient(circle, rgba(108,99,255,0.1) 0%, transparent 70%)', animation: 'floatA 20s ease-in-out infinite' }} />
      <div style={{ position: 'absolute', width: 400, height: 400, borderRadius: '50%', bottom: 0, right: -100, background: 'radial-gradient(circle, rgba(255,99,184,0.07) 0%, transparent 70%)', animation: 'floatB 25s ease-in-out infinite' }} />
      <div style={{ position: 'absolute', inset: 0, opacity: 0.15, backgroundImage: 'radial-gradient(circle, rgba(255,255,255,0.15) 1px, transparent 1px)', backgroundSize: '28px 28px' }} />
    </div>
  );
}

export function DashboardPage({ toast }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('links');
  const [showCreate, setShowCreate] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const links = useLinks();
  const backend = useBackendStatus();
  const { reload } = links;

  // Wake the Render backend; reload once a cold start finishes so the list isn't stuck on an error.
  useEffect(() => { wakeBackend(); }, []);
  useEffect(() => {
    if (backend.status === 'up' && backend.wasCold) reload();
  }, [backend.status, backend.wasCold, reload]);

  const handleLogout = useCallback(async () => {
    await logout();
    navigate('/');
  }, [logout, navigate]);

  const title = TABS.find(t => t.id === activeTab)?.label;

  return (
    <div className="dash">
      <AmbientBg />
      <Sidebar activeTab={activeTab} onSelect={setActiveTab} open={menuOpen} onClose={() => setMenuOpen(false)}
        user={user} onLogout={handleLogout} />

      <div className="dash-main">
        <header className="dash-topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
            <button className="bico dash-menu-btn" onClick={() => setMenuOpen(true)} aria-label="Open menu" aria-expanded={menuOpen}>☰</button>
            <h1 style={{ fontFamily: C.display, fontWeight: 800, fontSize: 18, color: C.text, letterSpacing: '-0.02em', whiteSpace: 'nowrap' }}>{title}</h1>
          </div>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <BackendStatusPill />
            {activeTab === 'links' && (
              <button className="btn-p btn-sm" onClick={() => setShowCreate(true)}>+ New link</button>
            )}
          </div>
        </header>

        <main className="dash-content">
          {!showCreate && <BackendStatusBanner />}
          {activeTab === 'links' && <LinksTab links={links} toast={toast} onCreate={() => setShowCreate(true)} />}
          {activeTab === 'analytics' && <AnalyticsTab links={links} />}
          {activeTab === 'qr' && (
            <Suspense fallback={<p style={{ fontFamily: C.mono, fontSize: 13, color: C.muted }}>Loading…</p>}>
              <QrToolsTab />
            </Suspense>
          )}
          {activeTab === 'settings' && <SettingsTab user={user} />}
        </main>
      </div>

      {showCreate && <CreateLinkPanel toast={toast} onCreated={reload} onClose={() => setShowCreate(false)} />}
    </div>
  );
}
