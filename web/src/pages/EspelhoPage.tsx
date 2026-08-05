import { useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type {
  EspelhoResponse,
  ServidorResponse,
  VinculoResponse,
  PendenciaFechamentoResponse,
} from '../api/types'
import { useEffect } from 'react'

function minToHHMM(min: number) {
  const h = Math.floor(Math.abs(min) / 60)
  const m = Math.abs(min) % 60
  return `${min < 0 ? '-' : ''}${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

export default function EspelhoPage() {
  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [vinculoId, setVinculoId] = useState('')
  const [competencia, setCompetencia] = useState(() => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  })
  const [espelho, setEspelho] = useState<EspelhoResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [loadingSrvs, setLoadingSrvs] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')
  const [pendencias, setPendencias] = useState<PendenciaFechamentoResponse | null>(null)
  const [loadingPend, setLoadingPend] = useState(false)
  const [selPend, setSelPend] = useState<Set<string>>(new Set())

  useEffect(() => {
    api.get<ServidorResponse[]>('/api/servidores')
      .then(setServidores)
      .catch(setError) // sem isto o seletor fica vazio e parece "nenhum servidor cadastrado"
      .finally(() => setLoadingSrvs(false))
  }, [])

  const vinculos: Array<{ vinculo: VinculoResponse; servidor: ServidorResponse }> = servidores.flatMap((s) =>
    s.vinculos.map((v) => ({ vinculo: v, servidor: s }))
  )

  async function buscar() {
    if (!vinculoId) return
    setLoading(true)
    setError(null)
    try {
      setEspelho(await api.get<EspelhoResponse>(`/api/espelho?vinculoId=${vinculoId}&competencia=${competencia}`))
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  async function fechar() {
    if (!vinculoId) return
    try {
      await api.post<unknown>(`/api/espelho/fechar?vinculoId=${vinculoId}&competencia=${competencia}`)
      setSucesso('Competência fechada.')
      setTimeout(() => setSucesso(''), 4000)
      void buscar()
    } catch (e) { setError(e) }
  }

  async function reabrir() {
    if (!vinculoId) return
    try {
      await api.post<unknown>(`/api/espelho/reabrir?vinculoId=${vinculoId}&competencia=${competencia}`, { motivo: 'Reabertura administrativa' })
      setSucesso('Competência reaberta.')
      setTimeout(() => setSucesso(''), 4000)
      void buscar()
    } catch (e) { setError(e) }
  }

  async function carregarPendencias() {
    setLoadingPend(true)
    setError(null)
    try {
      const p = await api.get<PendenciaFechamentoResponse>(`/api/espelho/pendentes?competencia=${competencia}`)
      setPendencias(p)
      setSelPend(new Set(p.orgaos.flatMap((o) => o.itens.map((i) => i.vinculoId))))
    } catch (e) {
      setError(e)
    } finally {
      setLoadingPend(false)
    }
  }

  function togglePend(id: string) {
    setSelPend((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  async function fecharLote() {
    const vinculoIds = [...selPend]
    if (vinculoIds.length === 0) return
    setLoadingPend(true)
    setError(null)
    try {
      const fechadas = await api.post<unknown[]>(`/api/espelho/fechar-lote?competencia=${competencia}`, { vinculoIds })
      setSucesso(`${fechadas.length} competência(s) fechada(s) em lote.`)
      setTimeout(() => setSucesso(''), 4000)
      await carregarPendencias()
    } catch (e) {
      setError(e)
    } finally {
      setLoadingPend(false)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Espelho / Fechamento' }]} />
      <h1 className="page-title">Espelho de Frequência</h1>
      <p className="page-subtitle">Consulte e feche competências mensais</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      {loadingSrvs ? <Loading label="Carregando servidores…" /> : (
        <div className="br-card" style={{ marginBottom: 24 }}>
          <div className="form-row" style={{ alignItems: 'flex-end', gap: 12 }}>
            <div className="br-form-group" style={{ marginBottom: 0 }}>
              <label className="br-label" htmlFor="vinculo-espelho">Vínculo</label>
              <select id="vinculo-espelho" className="br-select" value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
                <option value="">— Selecione —</option>
                {vinculos.map(({ vinculo, servidor }) => (
                  <option key={vinculo.id} value={vinculo.id}>
                    {servidor.nome} — {vinculo.matricula}
                  </option>
                ))}
              </select>
            </div>
            <div className="br-form-group" style={{ marginBottom: 0 }}>
              <label className="br-label" htmlFor="comp-espelho">Competência</label>
              <input
                id="comp-espelho"
                type="month"
                className="br-input"
                value={competencia}
                onChange={(e) => setCompetencia(e.target.value)}
              />
            </div>
            <div style={{ paddingBottom: 0 }}>
              <button className="br-button br-button--primary" onClick={() => void buscar()} disabled={!vinculoId || loading}>
                Consultar
              </button>
            </div>
          </div>
        </div>
      )}

      {loading && <Loading />}

      {espelho && (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <p className="stat-card__label">Total trabalhado</p>
              <p className="stat-card__value">{minToHHMM(espelho.totalMinutosTrabalhados)}</p>
            </div>
            <div className="stat-card">
              <p className="stat-card__label">Total esperado</p>
              <p className="stat-card__value">{minToHHMM(espelho.totalMinutosEsperados)}</p>
            </div>
            <div className="stat-card">
              <p className="stat-card__label">Saldo</p>
              <p className="stat-card__value" style={{ color: espelho.totalMinutosTrabalhados - espelho.totalMinutosEsperados >= 0 ? 'var(--color-success)' : 'var(--color-danger)' }}>
                {minToHHMM(espelho.totalMinutosTrabalhados - espelho.totalMinutosEsperados)}
              </p>
            </div>
            <div className="stat-card">
              <p className="stat-card__label">Status</p>
              <p className="stat-card__value" style={{ fontSize: 16 }}>{espelho.status}</p>
            </div>
          </div>

          <div className="action-bar">
            <div />
            <div className="action-bar__right">
              {espelho.status !== 'FECHADO' && (
                <button className="br-button br-button--primary" onClick={() => void fechar()}>
                  Fechar competência
                </button>
              )}
              {espelho.status === 'FECHADO' && (
                <button className="br-button br-button--secondary" onClick={() => void reabrir()}>
                  Reabrir
                </button>
              )}
            </div>
          </div>

          <div className="br-table-wrapper">
            <table className="br-table">
              <caption className="visually-hidden">Dias do espelho</caption>
              <thead>
                <tr>
                  <th scope="col">Data</th>
                  <th scope="col">Dia útil?</th>
                  <th scope="col">Trabalhado</th>
                  <th scope="col">Esperado</th>
                  <th scope="col">Justificado?</th>
                  <th scope="col">Ocorrências</th>
                </tr>
              </thead>
              <tbody>
                {espelho.dias.map((d) => (
                  <tr key={d.data}>
                    <td>{d.data}</td>
                    <td>{d.diaUtil ? 'Sim' : 'Não'}</td>
                    <td>{minToHHMM(d.minutosTrabalhados)}</td>
                    <td>{minToHHMM(d.minutosEsperados)}</td>
                    <td>{d.justificado ? <span className="br-badge br-badge--success">Sim</span> : '—'}</td>
                    <td>
                      {d.ocorrencias.length === 0 ? '—' :
                        d.ocorrencias.map((o, i) => (
                          <span key={i} className="br-badge br-badge--warning" style={{ marginRight: 4 }}>
                            {o.tipo} {minToHHMM(o.minutos)}
                          </span>
                        ))
                      }
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* Painel "o que falta fechar" + fechamento em lote (12.6.2) */}
      <div className="br-card" style={{ marginTop: 32 }}>
        <div className="action-bar" style={{ marginTop: 0 }}>
          <div className="action-bar__left">
            <h3 className="br-card__title" style={{ margin: 0 }}>O que falta fechar — {competencia}</h3>
          </div>
          <div className="action-bar__right">
            <button className="br-button br-button--secondary br-button--sm" onClick={() => void carregarPendencias()} disabled={loadingPend}>
              {loadingPend ? 'Carregando…' : 'Atualizar pendências'}
            </button>
          </div>
        </div>

        {pendencias && (
          <>
            <div className="stat-grid">
              <div className="stat-card">
                <p className="stat-card__label">Vínculos ativos</p>
                <p className="stat-card__value">{pendencias.totalVinculos}</p>
              </div>
              <div className="stat-card">
                <p className="stat-card__label">Fechadas</p>
                <p className="stat-card__value" style={{ color: 'var(--color-success)' }}>{pendencias.fechadas}</p>
              </div>
              <div className="stat-card">
                <p className="stat-card__label">Pendentes</p>
                <p className="stat-card__value" style={{ color: pendencias.pendentes > 0 ? 'var(--color-danger)' : 'var(--color-success)' }}>
                  {pendencias.pendentes}
                </p>
              </div>
            </div>

            {pendencias.orgaos.length === 0 ? (
              <Alert variant="success">Todas as competências do período estão fechadas.</Alert>
            ) : (
              <>
                <div className="action-bar">
                  <div className="action-bar__left">
                    <span>{selPend.size} vínculo(s) selecionado(s)</span>
                  </div>
                  <div className="action-bar__right">
                    <button className="br-button br-button--primary br-button--sm" onClick={() => void fecharLote()} disabled={selPend.size === 0 || loadingPend}>
                      Fechar selecionados em lote
                    </button>
                  </div>
                </div>
                {pendencias.orgaos.map((o) => (
                  <div key={o.lotacaoId ?? 'sem-orgao'} style={{ marginBottom: 16 }}>
                    <h4 style={{ fontSize: 14, margin: '8px 0' }}>
                      {o.orgao} <span className="br-badge br-badge--warning">{o.pendentes} pendente(s)</span>
                    </h4>
                    <div className="br-table-wrapper">
                      <table className="br-table">
                        <caption className="visually-hidden">Pendências de {o.orgao}</caption>
                        <thead>
                          <tr>
                            <th scope="col" style={{ width: 36 }}>Sel.</th>
                            <th scope="col">Servidor</th>
                            <th scope="col">Matrícula</th>
                          </tr>
                        </thead>
                        <tbody>
                          {o.itens.map((i) => (
                            <tr key={i.vinculoId}>
                              <td>
                                <input
                                  type="checkbox"
                                  aria-label={`Selecionar ${i.servidor}`}
                                  checked={selPend.has(i.vinculoId)}
                                  onChange={() => togglePend(i.vinculoId)}
                                />
                              </td>
                              <td>{i.servidor}</td>
                              <td>{i.matricula}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                ))}
              </>
            )}
          </>
        )}
      </div>
    </>
  )
}
