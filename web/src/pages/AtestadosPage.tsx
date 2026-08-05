import { useEffect, useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type { JustificativaResponse } from '../api/types'

const STATUS_LABELS: Record<string, string> = {
  PENDENTE: 'Pendente',
  APROVADA: 'Aprovada',
  REJEITADA: 'Rejeitada',
}

const STATUS_BADGE: Record<string, string> = {
  PENDENTE: 'br-badge--warning',
  APROVADA: 'br-badge--success',
  REJEITADA: 'br-badge--danger',
}

export default function AtestadosPage() {
  const [pendentes, setPendentes] = useState<JustificativaResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')
  const [processando, setProcessando] = useState<string | null>(null)
  const [selecionados, setSelecionados] = useState<Set<string>>(new Set())
  const [processandoLote, setProcessandoLote] = useState(false)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setPendentes(await api.get<JustificativaResponse[]>('/api/justificativas/pendentes'))
      setSelecionados(new Set())
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [])

  function flash(msg: string) {
    setSucesso(msg)
    setTimeout(() => setSucesso(''), 4000)
  }

  function toggle(id: string) {
    setSelecionados((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function toggleTodos() {
    setSelecionados((prev) =>
      prev.size === pendentes.length ? new Set() : new Set(pendentes.map((j) => j.id))
    )
  }

  async function decidir(id: string, acao: 'aprovar' | 'rejeitar', motivo?: string) {
    setProcessando(id)
    try {
      await api.post<unknown>(`/api/justificativas/${id}/${acao}`, { motivoDecisao: motivo ?? '' })
      flash(`Justificativa ${acao === 'aprovar' ? 'aprovada' : 'rejeitada'} com sucesso.`)
      void load()
    } catch (e) {
      setError(e)
    } finally {
      setProcessando(null)
    }
  }

  async function decidirLote(acao: 'aprovar' | 'rejeitar') {
    const ids = [...selecionados]
    if (ids.length === 0) return
    setProcessandoLote(true)
    setError(null)
    try {
      const motivoDecisao = acao === 'rejeitar' ? 'Recusado em lote' : 'Aprovado em lote'
      const decididas = await api.post<JustificativaResponse[]>(`/api/justificativas/lote/${acao}`, {
        ids,
        motivoDecisao,
      })
      flash(`${decididas.length} justificativa(s) ${acao === 'aprovar' ? 'aprovada(s)' : 'recusada(s)'} em lote.`)
      void load()
    } catch (e) {
      setError(e)
    } finally {
      setProcessandoLote(false)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Atestados / Justificativas' }]} />
      <h1 className="page-title">Atestados e Justificativas</h1>
      <p className="page-subtitle">Pendentes de aprovação pela chefia</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <Alert variant="info">
        Alçada: o gestor aprova ausências de até 5 dias; acima disso, a decisão sobe para o RH/controle.
      </Alert>

      {loading ? <Loading /> : pendentes.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">✅</div>
          <p className="empty-state__title">Nenhuma justificativa pendente</p>
          <p>Todas as justificativas foram processadas.</p>
        </div>
      ) : (
        <>
          <div className="action-bar">
            <div className="action-bar__left">
              <span>{selecionados.size} de {pendentes.length} selecionada(s)</span>
            </div>
            <div className="action-bar__right" style={{ display: 'flex', gap: 6 }}>
              <button
                className="br-button br-button--success br-button--sm"
                onClick={() => void decidirLote('aprovar')}
                disabled={selecionados.size === 0 || processandoLote}
              >
                Aprovar selecionados
              </button>
              <button
                className="br-button br-button--danger br-button--sm"
                onClick={() => void decidirLote('rejeitar')}
                disabled={selecionados.size === 0 || processandoLote}
              >
                Recusar selecionados
              </button>
            </div>
          </div>
          <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Justificativas pendentes</caption>
            <thead>
              <tr>
                <th scope="col" style={{ width: 36 }}>
                  <input
                    type="checkbox"
                    aria-label="Selecionar todas"
                    checked={selecionados.size === pendentes.length && pendentes.length > 0}
                    onChange={toggleTodos}
                  />
                </th>
                <th scope="col">Vínculo</th>
                <th scope="col">Tipo</th>
                <th scope="col">Período</th>
                <th scope="col">Motivo</th>
                <th scope="col">Status</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              {pendentes.map((j) => (
                <tr key={j.id}>
                  <td>
                    <input
                      type="checkbox"
                      aria-label={`Selecionar justificativa de ${j.vinculoId}`}
                      checked={selecionados.has(j.id)}
                      onChange={() => toggle(j.id)}
                    />
                  </td>
                  <td style={{ fontSize: 12, fontFamily: 'monospace' }}>{j.vinculoId}</td>
                  <td>{j.tipo}</td>
                  <td>{j.dataInicio} a {j.dataFim}</td>
                  <td style={{ maxWidth: 200 }}>{j.motivo ?? '—'}</td>
                  <td>
                    <span className={`br-badge ${STATUS_BADGE[j.status] ?? 'br-badge--neutral'}`}>
                      {STATUS_LABELS[j.status] ?? j.status}
                    </span>
                  </td>
                  <td>
                    {j.status === 'PENDENTE' && (
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button
                          className="br-button br-button--success br-button--sm"
                          onClick={() => void decidir(j.id, 'aprovar')}
                          disabled={processando === j.id}
                          aria-label={`Aprovar justificativa de ${j.vinculoId}`}
                        >
                          Aprovar
                        </button>
                        <button
                          className="br-button br-button--danger br-button--sm"
                          onClick={() => void decidir(j.id, 'rejeitar', 'Não autorizado')}
                          disabled={processando === j.id}
                          aria-label={`Rejeitar justificativa de ${j.vinculoId}`}
                        >
                          Rejeitar
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          </div>
        </>
      )}
    </>
  )
}
