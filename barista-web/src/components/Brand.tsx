export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <div className={compact ? 'brand compact' : 'brand'}>
      <span aria-hidden="true" className="brand-mark">SC</span>
      <div><strong>Social Cup</strong>{compact ? null : <span>Barista Check-In</span>}</div>
    </div>
  )
}
