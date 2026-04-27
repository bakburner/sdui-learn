import React, { useEffect, useState, useSyncExternalStore } from 'react';
import { createPortal } from 'react-dom';
import { subscribeToasts, getToastsSnapshot, type ToastItem } from '../runtime/ToastStore';

function useToasts(): ToastItem[] {
  return useSyncExternalStore(subscribeToasts, getToastsSnapshot, getToastsSnapshot);
}

export function ToastHost(): React.ReactElement | null {
  const [mounted, setMounted] = useState(false);
  const items = useToasts();

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted || typeof document === 'undefined') {
    return null;
  }

  return createPortal(
    <div style={styles.stack} aria-live="polite" aria-relevant="additions">
      {items.map((t) => (
        <div key={t.id} style={styles.toast}>
          {t.message}
        </div>
      ))}
    </div>,
    document.body,
  );
}

const styles: Record<string, React.CSSProperties> = {
  stack: {
    position: 'fixed',
    bottom: 72,
    left: '50%',
    transform: 'translateX(-50%)',
    zIndex: 10000,
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
    alignItems: 'center',
    maxWidth: '90vw',
    pointerEvents: 'none',
  },
  toast: {
    padding: '10px 20px',
    borderRadius: 8,
    backgroundColor: '#333',
    color: '#fff',
    fontSize: 13,
    textAlign: 'center',
    maxWidth: '90vw',
    animation: 'toastIn 0.3s ease',
    transition: 'opacity 0.3s ease',
  },
};
