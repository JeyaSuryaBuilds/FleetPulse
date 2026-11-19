import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { X, Loader2 } from 'lucide-react';
import { modalTransition } from './design-system/motion.js';

export function Button({ variant = 'primary', loading = false, className = '', children, disabled, type = 'button', ...rest }) {
  return (
    <button type={type} className={`fp-btn fp-btn--${variant} ${className}`} disabled={disabled || loading} {...rest}>
      {loading && <Loader2 size={16} className="animate-spin" aria-hidden />}
      {children}
    </button>
  );
}

export function Field({ label, error, hint, children, className = '' }) {
  return (
    <label className={`block ${className}`}>
      {label && <span className="mb-1.5 block text-sm text-fp-muted">{label}</span>}
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-fp-muted">{hint}</span>}
      {error && <span className="mt-1 block text-xs text-danger" role="alert">{error}</span>}
    </label>
  );
}

export const Input = ({ className = '', ...p }) => <input className={`fp-input ${className}`} {...p} />;
export const Select = ({ className = '', children, ...p }) => <select className={`fp-input ${className}`} {...p}>{children}</select>;
export const Textarea = ({ className = '', ...p }) => <textarea className={`fp-input ${className}`} rows={3} {...p} />;

export function Badge({ tone = 'brand', children }) {
  return <span className={`fp-badge fp-badge--${tone}`}>{children}</span>;
}

const STATUS_TONE = {
  ACTIVE: 'success', AVAILABLE: 'success', DELIVERED: 'success', COMPLETED: 'success', PAID: 'success', PRESENT: 'success', APPROVED: 'success', RESOLVED: 'success', SUCCEEDED: 'success',
  IN_USE: 'brand', IN_TRANSIT: 'brand', OUT_FOR_DELIVERY: 'brand', IN_PROGRESS: 'brand', TRIAL: 'brand', TRIALING: 'brand', ASSIGNED: 'brand', PICKED_UP: 'brand', ON_TRIP: 'brand',
  PENDING: 'warning', OPEN: 'warning', MAINTENANCE: 'warning', PLANNED: 'warning', DRAFT: 'warning', CREATED: 'warning', PAST_DUE: 'warning', NEW: 'warning', FINALIZED: 'warning',
  FAILED: 'error', CANCELLED: 'error', SUSPENDED: 'error', EXPIRED: 'error', REJECTED: 'error', DISABLED: 'error', RETIRED: 'error', ABSENT: 'error',
};
export const StatusBadge = ({ value }) => (value ? <Badge tone={STATUS_TONE[value] || 'brand'}>{String(value).replaceAll('_', ' ')}</Badge> : null);

export function Spinner({ label = 'Loading' }) {
  return <div className="flex items-center justify-center gap-2 py-10 text-fp-muted" role="status"><Loader2 className="animate-spin" size={18} /> {label}…</div>;
}

export function ErrorNote({ children }) {
  if (!children) return null;
  return <div role="alert" className="rounded-fp-md border border-danger/40 bg-danger/10 px-3 py-2 text-sm text-danger">{children}</div>;
}

export function EmptyState({ title, text }) {
  return (
    <div className="py-12 text-center">
      <p className="font-medium">{title}</p>
      {text && <p className="mt-1 text-sm text-fp-muted">{text}</p>}
    </div>
  );
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-fp-muted">{subtitle}</p>}
      </div>
      <div className="flex flex-wrap gap-2">{actions}</div>
    </div>
  );
}

export function Stat({ label, value, sub, tone }) {
  return (
    <div className="fp-card fp-card--padded">
      <p className="text-xs uppercase tracking-wide text-fp-muted">{label}</p>
      <p className={`mt-2 text-2xl font-semibold ${tone === 'error' ? 'text-danger' : tone === 'success' ? 'text-success' : ''}`}>{value ?? '—'}</p>
      {sub && <p className="mt-1 text-xs text-fp-muted">{sub}</p>}
    </div>
  );
}

export function Modal({ open, onClose, title, children, footer, wide = false }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => e.key === 'Escape' && onClose?.();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);
  return (
    <AnimatePresence>
      {open && (
        <motion.div className="fp-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onMouseDown={(e) => e.target === e.currentTarget && onClose?.()}>
          <motion.div className="fp-modal" style={wide ? { maxWidth: 760 } : undefined} role="dialog" aria-modal="true" aria-label={title} {...modalTransition}>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold">{title}</h2>
              <button className="fp-btn fp-btn--ghost" onClick={onClose} aria-label="Close"><X size={18} /></button>
            </div>
            <div className="max-h-[70vh] overflow-y-auto pr-1">{children}</div>
            {footer && <div className="mt-5 flex justify-end gap-2">{footer}</div>}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

const ToastCtx = createContext(() => {});
export const useToast = () => useContext(ToastCtx);
export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const push = useCallback((message, tone = 'success') => {
    const id = Math.random().toString(36).slice(2);
    setItems((l) => [...l, { id, message, tone }]);
    setTimeout(() => setItems((l) => l.filter((x) => x.id !== id)), 4500);
  }, []);
  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2" aria-live="polite">
        {items.map((t) => <div key={t.id} className="fp-toast" style={t.tone === 'error' ? { borderColor: 'var(--fp-error)' } : undefined}>{t.message}</div>)}
      </div>
    </ToastCtx.Provider>
  );
}

export function fmtDate(v) { if (!v) return '—'; const d = new Date(v); return isNaN(d) ? String(v) : d.toLocaleDateString(); }
export function fmtDateTime(v) { if (!v) return '—'; const d = new Date(v); return isNaN(d) ? String(v) : d.toLocaleString(); }
export function fmtMoney(v, currency = 'USD') { if (v === null || v === undefined || v === '') return '—'; try { return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(Number(v)); } catch { return String(v); } }
