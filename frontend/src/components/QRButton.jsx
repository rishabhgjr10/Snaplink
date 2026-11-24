import { lazy, Suspense, useState } from 'react';
import { Ic } from '../constants/icons';
import { formatDateTime } from '../lib/lifetime';

// The QR rendering library is only downloaded when a QR code is actually opened.
const QRCodeModal = lazy(() => import('./QRCodeModal').then(m => ({ default: m.QRCodeModal })));

/** Warning shown in the QR studio when the link will stop working. */
function qrNotice({ expiresAt, maxClicks }) {
  const parts = [];
  if (expiresAt) parts.push(`stops working on ${formatDateTime(expiresAt)}`);
  if (maxClicks) parts.push(`stops after ${maxClicks.toLocaleString()} clicks`);
  return parts.length ? `This link ${parts.join(' and ')}. A printed QR code will stop working too.` : null;
}

export function QRButton({ url, expiresAt = null, maxClicks = null }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button className="bico" onClick={() => setOpen(true)} title="Generate QR code" aria-label={`QR code for ${url}`}>
        {Ic.qr}
      </button>
      {open && (
        <Suspense fallback={null}>
          <QRCodeModal url={url} onClose={() => setOpen(false)} notice={qrNotice({ expiresAt, maxClicks })} />
        </Suspense>
      )}
    </>
  );
}
