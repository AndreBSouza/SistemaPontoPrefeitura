/** Formatação pt-BR compartilhada (evita duplicar `toLocaleString` espalhado pelas páginas). */

export function formatarDataHora(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export function formatarBRL(valor: number | null | undefined): string {
  if (valor == null) return '—'
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor)
}
