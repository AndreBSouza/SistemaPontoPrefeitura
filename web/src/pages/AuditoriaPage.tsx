import { useEffect, useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type { AuditoriaResponse } from '../api/types'

export default function AuditoriaPage() {
  const [eventos, setEventos] = useState<AuditoriaResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)

  useEffect(() => {
    api.get<AuditoriaResponse[]>('/api/auditoria')
      .then(setEventos)
      .catch(setError)
      .finally(() => setLoading(false))
  }, [])

  function formatDate(iso: string) {
    try {
      return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
    } catch { return iso }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Auditoria' }]} />
      <h1 className="page-title">Auditoria</h1>
      <p className="page-subtitle">Trilha de auditoria de todas as operações do sistema</p>

      {error && <ErrorAlert error={error} />}

      {loading ? <Loading /> : eventos.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">🔍</div>
          <p className="empty-state__title">Nenhum evento de auditoria</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Eventos de auditoria</caption>
            <thead>
              <tr>
                <th scope="col">Data/Hora</th>
                <th scope="col">Ação</th>
                <th scope="col">Entidade</th>
                <th scope="col">ID Entidade</th>
                <th scope="col">Ator</th>
                <th scope="col">Detalhe</th>
              </tr>
            </thead>
            <tbody>
              {eventos.map((e) => (
                <tr key={e.id}>
                  <td style={{ whiteSpace: 'nowrap', fontSize: 13 }}>{formatDate(e.ocorridoEm)}</td>
                  <td><span className="br-badge br-badge--info">{e.acao}</span></td>
                  <td>{e.entidade}</td>
                  <td style={{ fontFamily: 'monospace', fontSize: 11 }}>{e.entidadeId}</td>
                  <td>{e.ator}</td>
                  <td style={{ maxWidth: 240, fontSize: 13 }}>
                    {e.detalhe?.includes('fora-da-cerca') && (
                      <span
                        style={{
                          display: 'inline-block',
                          background: '#FFF3CD',
                          color: '#6B5400',
                          border: '1px solid #E0A800',
                          borderRadius: 100,
                          padding: '1px 8px',
                          fontSize: 11,
                          fontWeight: 600,
                          marginRight: 6,
                        }}
                      >
                        ⚠ fora da área
                      </span>
                    )}
                    {e.detalhe ?? '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
