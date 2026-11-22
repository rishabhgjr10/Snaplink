import { useEffect, useId, useRef } from 'react';

/**
 * Accessible modal: role="dialog", labelled by its title, closes on Escape and
 * on a click outside, moves focus inside on open and returns it on close.
 */
export function Modal({ title, onClose, children }) {
  const titleId = useId();
  const dialogRef = useRef(null);

  useEffect(() => {
    const previous = document.activeElement;
    const first = dialogRef.current?.querySelector('input, button, select, textarea, [href]');
    first?.focus();
    const onKey = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      if (previous instanceof HTMLElement) previous.focus();
    };
  }, [onClose]);

  return (
    <div className="modal-scrim" onMouseDown={e => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby={titleId} ref={dialogRef}>
        <h2 id={titleId}>{title}</h2>
        {children}
      </div>
    </div>
  );
}

/** Confirmation dialog replacing window.confirm(). */
export function ConfirmDialog({ title, message, confirmLabel = 'Confirm', danger = false, busy = false, onConfirm, onCancel }) {
  return (
    <Modal title={title} onClose={onCancel}>
      <p>{message}</p>
      <div className="modal-actions">
        <button className="btn-g btn-sm" onClick={onCancel} disabled={busy}>Cancel</button>
        <button className={danger ? 'btn-danger' : 'btn-p'} onClick={onConfirm} disabled={busy}>
          {busy ? 'Working…' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
