export function Loading({ label = 'Carregando…' }: { label?: string }) {
  return (
    <div className="loading-center" role="status" aria-label={label}>
      <div className="br-loading" aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}
