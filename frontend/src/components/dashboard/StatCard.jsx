import { useEffect, useState } from 'react';
import { C } from '../../constants/theme';

/** Animates from 0 to target; shows "—" until a value is known. */
function useCount(target, ms = 900) {
  const [val, setVal] = useState(0);
  useEffect(() => {
    if (typeof target !== 'number') return;
    if (target === 0) { setVal(0); return; }
    let current = 0;
    const step = Math.max(1, Math.ceil(target / (ms / 16)));
    const t = setInterval(() => {
      current += step;
      if (current >= target) { setVal(target); clearInterval(t); } else setVal(current);
    }, 16);
    return () => clearInterval(t);
  }, [target, ms]);
  return val;
}

export function StatCard({ label, value, variant = 'p', icon }) {
  const n = useCount(value);
  return (
    <div className="gcard" style={{ position: 'relative', padding: '18px 20px', display: 'flex', flexDirection: 'column', gap: 8, overflow: 'hidden' }}>
      <div className={`sc-${variant}`} />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <span style={{ fontFamily: C.mono, fontSize: 11, color: C.muted, letterSpacing: '.06em', textTransform: 'uppercase' }}>{label}</span>
        {icon && <span aria-hidden="true" style={{ fontSize: 16, opacity: 0.5 }}>{icon}</span>}
      </div>
      <div style={{ fontFamily: C.display, fontWeight: 800, fontSize: 30, color: C.text, letterSpacing: '-0.03em', lineHeight: 1 }}>
        {typeof value === 'number' ? n.toLocaleString() : '—'}
      </div>
    </div>
  );
}
