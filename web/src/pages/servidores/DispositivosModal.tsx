import { useEffect, useState } from 'react'
import { Modal } from '../../components/Modal'
import { Alert, ErrorAlert } from '../../components/Alert'
import { Loading } from '../../components/Loading'
import { api } from '../../api/client'
import { formatarDataHora } from '../../utils/formato'
import type { DispositivoResponse } from '../../api/types'

export function DispositivosModal({ vinculoId, onClose }: {
  vinculoId: string
  onClose: () => void
}) {
  const [dispositivos, setDispositivos] = useState<DispositivoResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [revogando, setRevogando] = useState<string | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')

  async function carregar() {
    setLoading(true)
    setError(null)
    try {
      const res = await api.get<DispositivoResponse[]>(`/api/ativacao/dispositivos?vinculoId=${vinculoId}`)
      setDispositivos(res)
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void carregar() }, [])

  async function revogar(id: string, nome: string | null) {
    const label = nome ?? 'este dispositivo'
    if (!window.confirm(`Confirma a revogação de ${label}? O servidor precisará se cadastrar novamente.`)) return
    setRevogando(id)
    setError(null)
    try {
      await api.post<void>(`/api/ativacao/dispositivos/${id}/revogar`, {})
      setSucesso(`Dispositivo ${label} revogado.`)
      setTimeout(() => setSucesso(''), 4000)
      await carregar()
    } catch (err) {
      setError(err)
    } finally {
      setRevogando(null)
    }
  }

  const ativos = dispositivos.filter((d) => d.ativo).length

  return (
    <Modal
      title="Dispositivos Vinculados"
      onClose={onClose}
      footer={
        <button className="br-button br-button--secondary" onClick={onClose}>Fechar</button>
      }
    >
      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error != null && <ErrorAlert error={error} />}

      {loading ? (
        <Loading label="Carregando dispositivos…" />
      ) : dispositivos.length === 0 ? (
        <div className="empty-state" style={{ padding: '24px 0' }}>
          <div className="empty-state__icon" aria-hidden="true">📱</div>
          <p className="empty-state__title">Nenhum dispositivo cadastrado</p>
          <p>Gere um código de ativação para vincular o primeiro aparelho.</p>
        </div>
      ) : (
        <>
          <p style={{ marginBottom: 12, fontSize: 13, color: 'var(--color-gray-60)' }}>
            {ativos} dispositivo(s) ativo(s) de {dispositivos.length} no total.
          </p>
          <div className="br-table-wrapper">
            <table className="br-table">
              <caption className="visually-hidden">Dispositivos vinculados ao servidor</caption>
              <thead>
                <tr>
                  <th scope="col">Nome do aparelho</th>
                  <th scope="col">Situação</th>
                  <th scope="col">Vinculado em</th>
                  <th scope="col">Ação</th>
                </tr>
              </thead>
              <tbody>
                {dispositivos.map((d) => (
                  <tr key={d.id}>
                    <td>{d.nome ?? <em style={{ color: 'var(--color-gray-40)' }}>Sem nome</em>}</td>
                    <td>
                      {d.ativo ? (
                        <span className="br-badge br-badge--success">Ativo</span>
                      ) : (
                        <span className="br-badge br-badge--neutral">Revogado</span>
                      )}
                    </td>
                    <td style={{ fontSize: 13 }}>{formatarDataHora(d.criadoEm)}</td>
                    <td>
                      {d.ativo && (
                        <button
                          className="br-button br-button--sm br-button--danger"
                          onClick={() => void revogar(d.id, d.nome)}
                          disabled={revogando === d.id}
                          aria-label={`Revogar acesso do dispositivo ${d.nome ?? d.id}`}
                        >
                          {revogando === d.id ? 'Revogando…' : 'Revogar'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </Modal>
  )
}
