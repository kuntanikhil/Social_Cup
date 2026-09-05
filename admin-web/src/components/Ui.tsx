import { useEffect, type PropsWithChildren, type ReactNode } from 'react'

export function PageHeader({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <header className="page-header"><div><h1>{title}</h1><p>{description}</p></div>{action}</header>
}

export function StatePanel({ title, message, action }: { title: string; message: string; action?: ReactNode }) {
  return <div className="state-panel"><span aria-hidden="true">○</span><h2>{title}</h2><p>{message}</p>{action}</div>
}

export function LoadingPanel({ label = 'Loading…' }: { label?: string }) {
  return <div aria-live="polite" className="loading-panel"><span className="spinner" />{label}</div>
}

export function StatusBadge({ active, activeLabel = 'Active', inactiveLabel = 'Inactive' }: { active: boolean; activeLabel?: string; inactiveLabel?: string }) {
  return <span className={`status-badge ${active ? 'success' : 'neutral'}`}><i />{active ? activeLabel : inactiveLabel}</span>
}

export function Alert({ children, tone = 'error' }: PropsWithChildren<{ tone?: 'error' | 'success' | 'info' }>) {
  return <div aria-live="polite" className={`alert ${tone}`} role={tone === 'error' ? 'alert' : 'status'}>{children}</div>
}

export function Modal({ title, children, onClose }: PropsWithChildren<{ title: string; onClose: () => void }>) {
  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose() }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onClose])

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section aria-labelledby="modal-title" aria-modal="true" className="modal" role="dialog">
        <header><h2 id="modal-title">{title}</h2><button aria-label="Close dialog" onClick={onClose} type="button">×</button></header>
        {children}
      </section>
    </div>
  )
}
