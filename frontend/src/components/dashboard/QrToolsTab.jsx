import { C } from '../../constants/theme';
import { QRBatch } from '../QRBatch';
import { QRDecodePanel } from '../QRDecodePanel';

/**
 * Client-side QR utilities. A QR code for a single link is available from the
 * QR button on each row in Short Links.
 */
export function QrToolsTab() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, animation: 'fadeUp .3s both' }}>
      <p style={{ fontFamily: C.mono, fontSize: 12, color: C.muted, lineHeight: 1.6 }}>
        These tools run entirely in your browser. To get a QR code for one of your links, use the ▦ button on its row.
      </p>
      <details className="gcard" style={{ padding: '4px 0' }} open>
        <summary className="accbtn" style={{ listStyle: 'none', border: 'none', background: 'none' }}>
          <span>⊞ Batch QR image export</span><span aria-hidden="true">▼</span>
        </summary>
        <div style={{ padding: 16 }}>
          <QRBatch baseOptions={{ dotType: 'rounded', cornerSq: 'extra-rounded', cornerDot: 'dot', dark: '#0a0a0a', light: '#6C63FF', ecLevel: 'M' }} />
        </div>
      </details>
      <details className="gcard" style={{ padding: '4px 0' }}>
        <summary className="accbtn" style={{ listStyle: 'none', border: 'none', background: 'none' }}>
          <span>⊙ Decode QR from image</span><span aria-hidden="true">▼</span>
        </summary>
        <div style={{ padding: 16 }}>
          <QRDecodePanel />
        </div>
      </details>
    </div>
  );
}
