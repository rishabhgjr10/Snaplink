import { C } from '../constants/theme';
import { MAINTENANCE_MODE, PORTFOLIO_URL } from '../config/maintenance';

const AMBER = '#fbbf24';

/**
 * Bold notice that SnapLink is offline while its database moves to a new host.
 * Renders nothing unless MAINTENANCE_MODE is on (see config/maintenance.js).
 * `compact` is for tighter layouts (sign-in card, dashboard).
 */
export function MaintenanceBanner({ compact = false, style }) {
  if (!MAINTENANCE_MODE) return null;

  return (
    <section
      role="alert"
      aria-labelledby="maintenance-title"
      style={{
        position: 'relative', zIndex: 10, width: '100%', maxWidth: compact ? 640 : 760,
        margin: '0 auto', textAlign: 'left',
        background: 'linear-gradient(135deg, rgba(251,191,36,0.16) 0%, rgba(251,191,36,0.06) 100%)',
        border: '2px solid rgba(251,191,36,0.55)', borderRadius: 16,
        boxShadow: '0 0 40px rgba(251,191,36,0.12)',
        padding: compact ? '16px 18px' : '22px 26px',
        display: 'flex', flexDirection: 'column', gap: compact ? 8 : 12,
        ...style,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <span aria-hidden="true" style={{ fontSize: compact ? 20 : 26, lineHeight: 1 }}>🚧</span>
        <h2 id="maintenance-title" style={{
          fontFamily: C.display, fontWeight: 800, color: AMBER, margin: 0,
          fontSize: compact ? 17 : 'clamp(1.25rem, 3vw, 1.7rem)', letterSpacing: '-0.01em', lineHeight: 1.2,
        }}>
          SnapLink is temporarily offline
        </h2>
      </div>

      <p style={{ fontFamily: C.space, fontSize: compact ? 13.5 : 15.5, color: C.text, lineHeight: 1.6, margin: 0 }}>
        We are moving our database to a new home. Until the move is finished, <strong>creating links,
        the dashboard and existing short links will not work</strong>. Thanks for your patience.
      </p>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap', marginTop: compact ? 2 : 4 }}>
        <span style={{ fontFamily: C.mono, fontSize: 12, color: 'rgba(240,236,255,0.78)' }}>
          In the meantime, check out my other applications:
        </span>
        <a
          href={PORTFOLIO_URL}
          target="_blank"
          rel="noopener noreferrer"
          style={{
            display: 'inline-flex', alignItems: 'center', gap: 6,
            background: AMBER, color: '#1a1206', fontFamily: C.mono, fontWeight: 700, fontSize: 13,
            padding: '8px 16px', borderRadius: 9, textDecoration: 'none', whiteSpace: 'nowrap',
          }}
        >
          Visit my portfolio ↗
        </a>
      </div>
    </section>
  );
}
