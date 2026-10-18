import { useState } from 'react';
import { Link } from 'react-router-dom';
import { C } from '../constants/theme';
import { GrainBg } from '../components/GrainBg';
import { Footer, NavBar } from '../layouts/MainLayout';
import { shortenUrl } from '../services/api';
import { useAuth } from '../context/AuthContext';

/* ── Orb blobs for hero depth ─────────────────────────────────────────────── */
function Orbs() {
  return (
    <div style={{ position: 'absolute', inset: 0, overflow: 'hidden', pointerEvents: 'none', zIndex: 1 }}>
      <div style={{ position: 'absolute', width: 700, height: 700, borderRadius: '50%', top: -200, left: '50%', transform: 'translateX(-55%)', background: 'radial-gradient(circle, rgba(108,99,255,0.13) 0%, transparent 70%)', animation: 'floatA 18s ease-in-out infinite' }} />
      <div style={{ position: 'absolute', width: 500, height: 500, borderRadius: '50%', top: 100, right: -150, background: 'radial-gradient(circle, rgba(255,99,184,0.09) 0%, transparent 70%)', animation: 'floatB 22s ease-in-out infinite' }} />
      <div style={{ position: 'absolute', width: 400, height: 400, borderRadius: '50%', bottom: -100, left: -80, background: 'radial-gradient(circle, rgba(164,246,112,0.06) 0%, transparent 70%)', animation: 'floatC 25s ease-in-out infinite' }} />
    </div>
  );
}

/* ── Dot grid overlay ─────────────────────────────────────────────────────── */
function DotGrid() {
  return (
    <div style={{
      position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 2, opacity: 0.35,
      backgroundImage: 'radial-gradient(circle, rgba(255,255,255,0.18) 1px, transparent 1px)',
      backgroundSize: '32px 32px',
      maskImage: 'radial-gradient(ellipse 80% 60% at 50% 40%, #000 40%, transparent 100%)',
      WebkitMaskImage: 'radial-gradient(ellipse 80% 60% at 50% 40%, #000 40%, transparent 100%)',
    }} />
  );
}

/* ── Vignette ─────────────────────────────────────────────────────────────── */
function Vignette() {
  return <div style={{ position: 'absolute', inset: 0, pointerEvents: 'none', zIndex: 3, background: 'radial-gradient(ellipse 100% 100% at 50% 0%, transparent 40%, rgba(7,6,15,0.7) 100%)' }} />;
}

/* ── Demo widget ──────────────────────────────────────────────────────────── */
function DemoWidget() {
  const { user, isAuthLoading } = useAuth();
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  async function handleShorten(e) {
    e.preventDefault();
    if (!url.trim()) return;
    setLoading(true); setError(''); setResult(null);
    try {
      const data = await shortenUrl({ url: url.trim() });
      setResult(data);
    } catch (err) {
      setError(err.message || 'Could not shorten URL. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="su d4" style={{ width: '100%', maxWidth: 580, background: 'rgba(255,255,255,0.04)', backdropFilter: 'blur(16px)', WebkitBackdropFilter: 'blur(16px)', border: '1px solid rgba(255,255,255,0.09)', borderRadius: 16, padding: '22px 24px', display: 'flex', flexDirection: 'column', gap: 14 }}>
      <form onSubmit={handleShorten} style={{ display: 'flex', gap: 10 }}>
        <input
          className="inp"
          placeholder="Paste a long URL here…"
          value={url}
          onChange={e => setUrl(e.target.value)}
          style={{ flex: 1 }}
        />
        <button type="submit" className="btn-p" disabled={loading} style={{ textDecoration: 'none', flexShrink: 0, cursor: 'pointer', border: 'none' }}>
          {loading ? '…' : 'Shorten →'}
        </button>
      </form>
      {error && <p style={{ fontFamily: C.mono, fontSize: 12, color: '#ff6b6b', margin: 0 }}>{error}</p>}
      {result && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: 'rgba(108,99,255,0.08)', border: '1px solid rgba(108,99,255,0.22)', borderRadius: 8, padding: '10px 14px' }}>
          <a href={result.short_url} target="_blank" rel="noreferrer"
            style={{ fontFamily: C.mono, fontSize: 13, color: C.accent, textDecoration: 'none', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {result.short_url}
          </a>
          <button onClick={() => navigator.clipboard.writeText(result.short_url)}
            style={{ background: 'none', border: `1px solid rgba(108,99,255,0.4)`, borderRadius: 5, color: C.accent, fontFamily: C.mono, fontSize: 11, padding: '3px 8px', cursor: 'pointer' }}>
            Copy
          </button>
        </div>
      )}
      <p style={{ fontFamily: C.mono, fontSize: 11, color: C.muted, margin: 0, minHeight: 18 }}>
        {user?.email ? (
          <span>
            Logged in as <strong style={{ color: C.text, fontWeight: 500 }}>{user.email}</strong> • Links auto-sync to your account
          </span>
        ) : isAuthLoading ? (
          <span style={{ opacity: 0 }}>Free to use · No sign-up required</span>
        ) : (
          'Free to use • No sign-up required'
        )}
      </p>
    </div>
  );
}

/* ── Glassmorphic Feature Pills Row ───────────────────────────────────────── */
const FEATURE_PILLS = [
  {
    label: 'Custom Slugs',
    icon: (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#a855f7', flexShrink: 0 }}>
        <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
        <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
      </svg>
    ),
  },
  {
    label: 'QR Codes',
    icon: (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#ec4899', flexShrink: 0 }}>
        <rect width="5" height="5" x="3" y="3" rx="1"/>
        <rect width="5" height="5" x="16" y="3" rx="1"/>
        <rect width="5" height="5" x="3" y="16" rx="1"/>
        <path d="M21 16h-3a2 2 0 0 0-2 2v3"/>
        <path d="M21 21v.01"/>
        <path d="M12 7v3a2 2 0 0 1-2 2H7"/>
        <path d="M3 12h.01"/>
        <path d="M12 3h.01"/>
        <path d="M12 16v.01"/>
        <path d="M16 12h1"/>
        <path d="M21 12v.01"/>
        <path d="M12 21v-1"/>
      </svg>
    ),
  },
  {
    label: 'Click Analytics',
    icon: (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#22c55e', flexShrink: 0 }}>
        <line x1="12" x2="12" y1="20" y2="10"/>
        <line x1="18" x2="18" y1="20" y2="4"/>
        <line x1="6" x2="6" y1="20" y2="16"/>
      </svg>
    ),
  },
  {
    label: 'OAuth Login',
    icon: (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#eab308', flexShrink: 0 }}>
        <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/>
      </svg>
    ),
  },
];

function FeaturePillsRow() {
  return (
    <div
      className="w-full max-w-4xl mx-auto mt-12 mb-20 flex flex-wrap items-center justify-center gap-3 sm:gap-6 px-4"
      style={{
        width: '100%',
        maxWidth: 896,
        margin: '48px auto 80px',
        display: 'flex',
        flexWrap: 'wrap',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 16,
        padding: '0 16px',
        position: 'relative',
        zIndex: 10,
      }}
    >
      {FEATURE_PILLS.map((f) => (
        <span
          key={f.label}
          className="flex items-center gap-2 px-4 py-1.5 rounded-full border border-white/5 bg-white/[0.03] text-sm font-medium text-white/50 backdrop-blur-sm shadow-sm transition-colors hover:text-white/80 hover:bg-white/[0.05]"
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 8,
            padding: '6px 16px',
            borderRadius: 9999,
            border: '1px solid rgba(255, 255, 255, 0.08)',
            background: 'rgba(255, 255, 255, 0.03)',
            fontSize: 13.5,
            fontWeight: 500,
            color: 'rgba(255, 255, 255, 0.55)',
            backdropFilter: 'blur(8px)',
            WebkitBackdropFilter: 'blur(8px)',
            boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)',
            transition: 'all 0.2s cubic-bezier(0.22, 1, 0.36, 1)',
            fontFamily: C.mono,
            cursor: 'default',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.color = 'rgba(255, 255, 255, 0.85)';
            e.currentTarget.style.background = 'rgba(255, 255, 255, 0.06)';
            e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.16)';
            e.currentTarget.style.transform = 'translateY(-1px)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.color = 'rgba(255, 255, 255, 0.55)';
            e.currentTarget.style.background = 'rgba(255, 255, 255, 0.03)';
            e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.08)';
            e.currentTarget.style.transform = 'none';
          }}
        >
          {f.icon}
          <span>{f.label}</span>
        </span>
      ))}
    </div>
  );
}

/* ── Feature cards ────────────────────────────────────────────────────────── */
const FEATURES = [
  { title: 'Custom Slugs', desc: 'Choose your own short code or let the system generate a unique one.', icon: '✦', variant: '' },
  { title: 'QR Codes', desc: 'Instant QR code for every shortened link — ready to download and share.', icon: '▦', variant: 'pk' },
  { title: 'Click Analytics', desc: 'Track total clicks and view recent visit history for every link.', icon: '◉', variant: '' },
  { title: 'OAuth Login', desc: 'One-click sign in with Google or GitHub via Firebase Auth.', icon: '◌', variant: 'lm' },
];

/* ── Main Page ────────────────────────────────────────────────────────────── */
export function LandingPage() {
  const { user, isAuthLoading } = useAuth();

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: C.bg, position: 'relative' }}>
      <GrainBg />
      <NavBar />

      {/* ── Hero ── */}
      <section style={{ position: 'relative', zIndex: 10, minHeight: '86vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '120px 24px 30px', textAlign: 'center', gap: 28 }}>
        <Orbs />
        <DotGrid />
        <Vignette />

        {/* headline container */}
        <div style={{ position: 'relative', zIndex: 10, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 18, maxWidth: 840 }}>
          
          {/* Top Brand Pill */}
          <div
            className="su text-purple-300 bg-purple-950/50 border border-purple-500/30 text-xs font-medium px-4 py-1.5 rounded-full shadow-[0_0_12px_rgba(168,85,247,0.15)]"
            style={{
              display: 'inline-flex', alignItems: 'center', gap: 8,
              background: 'rgba(59, 7, 100, 0.5)', border: '1px solid rgba(168, 85, 247, 0.3)',
              borderRadius: 9999, padding: '6px 16px', fontFamily: C.mono, fontSize: 12,
              color: '#d8b4fe', letterSpacing: '.06em',
              boxShadow: '0 0 12px rgba(168,85,247,0.15)',
            }}
          >
            ⚡ SnapLink by bauna.xyz • Ultra-Fast URL Shortener
          </div>

          {/* Main Title */}
          <h1 className="su d1" style={{
            fontSize: 'clamp(2.75rem,6.5vw,5.2rem)', fontFamily: C.display, fontWeight: 800,
            color: C.text, lineHeight: 1.05, letterSpacing: '-0.04em', margin: 0,
          }}>
            Shorten. Track.{' '}
            <span style={{ background: 'linear-gradient(135deg, #6C63FF 0%, #FF63B8 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text' }}>
              Launch.
            </span>
          </h1>

          {/* Subtitle */}
          <p className="su d2" style={{ fontFamily: C.space, fontSize: 17, color: C.muted, lineHeight: 1.7, maxWidth: 540, margin: '0 auto' }}>
            A fast, free URL shortener. Custom slugs, click analytics, QR codes, and OAuth login — powered by Spring Boot &amp; Redis.
          </p>

          {/* 100% Free & Open Source + GitHub Star Button (Task 6) */}
          <div className="su d2" style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap', justifyContent: 'center', marginTop: 4 }}>
            <span style={{
              display: 'inline-flex', alignItems: 'center', gap: 6,
              background: 'rgba(164,246,112,0.08)', border: '1px solid rgba(164,246,112,0.22)',
              color: '#A4F670', fontFamily: C.mono, fontSize: 12, fontWeight: 600,
              padding: '6px 14px', borderRadius: 9999,
            }}>
              ✨ 100% Free &amp; Open Source
            </span>
            <a
              href="https://github.com/rishabhgjr10/Snaplink"
              target="_blank"
              rel="noreferrer"
              className="bg-white/5 hover:bg-white/10 border border-white/10 text-sm font-medium px-4 py-1.5 rounded-full flex items-center gap-2 transition-all duration-200"
              style={{
                display: 'inline-flex', alignItems: 'center', gap: 8,
                background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.12)',
                borderRadius: 9999, padding: '6px 16px', color: C.text,
                fontFamily: C.mono, fontSize: 12, textDecoration: 'none',
                transition: 'all .2s cubic-bezier(.22,1,.36,1)',
              }}
              onMouseEnter={e => {
                e.currentTarget.style.background = 'rgba(255,255,255,0.1)';
                e.currentTarget.style.borderColor = 'rgba(255,255,255,0.25)';
                e.currentTarget.style.boxShadow = '0 0 20px rgba(108,99,255,0.25)';
              }}
              onMouseLeave={e => {
                e.currentTarget.style.background = 'rgba(255,255,255,0.05)';
                e.currentTarget.style.borderColor = 'rgba(255,255,255,0.12)';
                e.currentTarget.style.boxShadow = 'none';
              }}
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
              </svg>
              <span>Star on GitHub ↗</span>
            </a>
          </div>

          {/* Action Buttons for Logged-Out Visitors (Fixed min-height preserves layout while loading) */}
          {!user && (
            <div
              className="su d3 min-h-[44px] flex items-center justify-center"
              style={{
                minHeight: 44,
                display: 'flex',
                gap: 12,
                flexWrap: 'wrap',
                justifyContent: 'center',
                marginTop: 4,
              }}
            >
              {!isAuthLoading && !user && (
                <>
                  <Link to="/signup" className="btn-p">Get started free →</Link>
                  <Link to="/login" className="btn-g">Sign in</Link>
                </>
              )}
            </div>
          )}

          {/* Direct URL Shortening Input */}
          <DemoWidget />
        </div>
      </section>

      {/* ── Floating Glassmorphic Feature Row ── */}
      <FeaturePillsRow />

      {/* ── Features bento ── */}
      <section id="features" style={{ position: 'relative', zIndex: 10, padding: '90px 40px', maxWidth: 1100, margin: '0 auto', width: '100%' }}>
        <div className="su" style={{ textAlign: 'center', marginBottom: 50 }}>
          <h2 style={{ fontFamily: C.display, fontWeight: 800, fontSize: 'clamp(2rem,4vw,3rem)', color: C.text, letterSpacing: '-0.03em', marginBottom: 12 }}>
            Everything you need
          </h2>
          <p style={{ fontFamily: C.space, fontSize: 16, color: C.muted }}>
            Essential link tools engineered for speed and reliability.
          </p>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 16, gridAutoRows: 'auto' }}>
          {FEATURES.map((f, i) => (
            <div key={f.title} className={`fc su d${Math.min(i + 1, 7)}${f.variant ? ' ' + f.variant : ''}${f.wide ? ' wide' : ''}`}>
              <span style={{ fontSize: 22 }}>{f.icon}</span>
              <span style={{ fontFamily: C.space, fontWeight: 600, fontSize: 15, color: C.text }}>{f.title}</span>
              <span style={{ fontFamily: C.space, fontSize: 13, color: C.muted, lineHeight: 1.6 }}>{f.desc}</span>
            </div>
          ))}
        </div>
      </section>

      {/* Bottom open-source CTA section removed per Task 7 (now proudly presented in Hero) */}

      <Footer />
    </div>
  );
}
