import { useEffect, useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type { SolicitacaoEnte, OnboardingProvisionado } from '../api/types'

/** Fila de solicitações de adesão self-service (12.3.13) — aprovação pelo operador da plataforma. */
export default function OnboardingEntesPage() {
  const [solicitacoes, setSolicitacoes] = useState<SolicitacaoEnte[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)
  const [agindo, setAgindo] = useState<string | null>(null)

  function load() {
    setLoading(true)
    api
      .get<SolicitacaoEnte[]>('/api/onboarding/solicitacoes')
      .then(setSolicitacoes)
      .catch(setError)
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  async function aprovar(s: SolicitacaoEnte) {
    setAgindo(s.id)
    setError(null)
    try {
      const prov = await api.post<OnboardingProvisionado>(`/api/onboarding/solicitacoes/${s.id}/aprovar`, {})
      setSucesso(`Ente "${s.nome}" provisionado (${prov.slug}).`)
      load()
    } catch (err) {
      setError(err)
    } finally {
      setAgindo(null)
    }
  }

  async function rejeitar(s: SolicitacaoEnte) {
    setAgindo(s.id)
    setError(null)
    try {
      await api.post<void>(`/api/onboarding/solicitacoes/${s.id}/rejeitar`, {})
      setSucesso(`Solicitação de "${s.nome}" rejeitada.`)
      load()
    } catch (err) {
      setError(err)
    } finally {
      setAgindo(null)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Adesão de entes' }]} />
      <h1 className="page-title">Adesão de entes</h1>
      <p className="page-subtitle">Solicitações self-service aguardando aprovação do operador</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error != null && <ErrorAlert error={error} />}

      {loading ? (
        <Loading />
      ) : solicitacoes.length === 0 ? (
        <div className="empty-state"><p className="empty-state__title">Nenhuma solicitação pendente</p></div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Solicitações de adesão</caption>
            <thead>
              <tr>
                <th scope="col">Ente</th>
                <th scope="col">Identificador</th>
                <th scope="col">Poder</th>
                <th scope="col">Responsável</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              {solicitacoes.map((s) => (
                <tr key={s.id}>
                  <td>{s.nome}</td>
                  <td style={{ fontFamily: 'monospace' }}>{s.slug}</td>
                  <td>{s.tipoPoder}</td>
                  <td>{s.responsavelNome}<br /><span style={{ fontSize: 12, color: 'var(--color-gray-60)' }}>{s.responsavelEmail}</span></td>
                  <td>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button className="br-button br-button--primary br-button--sm"
                              disabled={agindo === s.id} onClick={() => void aprovar(s)}>
                        {agindo === s.id ? '…' : 'Aprovar e provisionar'}
                      </button>
                      <button className="br-button br-button--danger br-button--sm"
                              disabled={agindo === s.id} onClick={() => void rejeitar(s)}>
                        Rejeitar
                      </button>
                    </div>
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
