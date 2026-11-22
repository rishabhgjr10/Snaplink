import { C } from '../constants/theme';

export function ToastStack({ toasts }) {
  return (
    <div role="status" aria-live="polite" style={{
      position: 'fixed', bottom: 16, right: 16, left: 'auto', zIndex: 9999,
      display: 'flex', flexDirection: 'column', gap: 8, maxWidth: 'calc(100vw - 32px)',
    }}>
      {toasts.map(t => (
        <div key={t.id} style={{
          background: t.type === 'error' ? 'rgba(40,12,16,0.92)' : 'rgba(20,18,40,0.92)',
          border: `1px solid ${t.type === 'error' ? 'rgba(255,68,68,0.35)' : 'rgba(108,99,255,0.35)'}`,
          backdropFilter: 'blur(12px)',
          WebkitBackdropFilter: 'blur(12px)',
          borderRadius: 9,
          color: t.type === 'error' ? C.error : C.text,
          padding: '10px 18px', fontSize: 13, lineHeight: 1.5,
          fontFamily: C.mono, maxWidth: 360, whiteSpace: 'normal', overflowWrap: 'anywhere',
          animation: 'toastIn .2s ease',
        }}>{t.msg}</div>
      ))}
    </div>
  );
}
