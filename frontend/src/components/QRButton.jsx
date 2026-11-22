import { lazy, Suspense, useState } from 'react';
import { Ic } from '../constants/icons';

// The QR rendering library is only downloaded when a QR code is actually opened.
const QRCodeModal = lazy(() => import('./QRCodeModal').then(m => ({ default: m.QRCodeModal })));

export function QRButton({ url }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button className="bico" onClick={() => setOpen(true)} title="Generate QR code" aria-label={`QR code for ${url}`}>
        {Ic.qr}
      </button>
      {open && (
        <Suspense fallback={null}>
          <QRCodeModal url={url} onClose={() => setOpen(false)} />
        </Suspense>
      )}
    </>
  );
}
