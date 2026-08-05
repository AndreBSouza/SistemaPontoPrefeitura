import { useEffect, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type { DelegacaoResponse, CriarDelegacaoRequest, ServidorResponse } from '../api/types'

export default function DelegacoesPage() {
  const [delegacoes, setDelegacoes] = useState<DelegacaoResponse[]>([])
  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')

  const [delegante, setDelegante] = useState('')
  const [delegado, setDelegado] = useState('')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [saving, setSaving] = useState(false)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [d, s] = await Promise.all([
        api.get<DelegacaoResponse[]>('/api/delegacoes'),
        api.get<ServidorResponse[]>('/api/servidores'),
      ])
      setDelegacoes(d)
      setServidores(s)
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

  const nomeServidor = (id: string) => servidores.find((s) => s.id === id)?.nome ?? id

  async function criar(e: FormEvent) {
    e.preventDefault()
    if (!delegante || !delegado || !dataInicio || !dataFim) return
    setSaving(true)
    setError(null)
    try {
      const body: CriarDelegacaoRequest = {
        deleganteServidorId: delegante,
        delegadoServidorId: delegado,
        dataInicio,
        dataFim,
      }
      await api.post<DelegacaoResponse>('/api/delegacoes', body)
      flash('Delegação criada.')
      setDelegante('')
      setDelegado('')
      setDataInicio('')
      setDataFim('')
      void load()
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function revogar(id: string) {
    try {
      await api.delete<void>(`/api/delegacoes/${id}`)
      flash('Delegação revogada.')
      void load()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Delegação de aprovação' }]} />
      <h1 className="page-title">Delegação de aprovação</h1>
      <p className="page-subtitle">Substituto do gestor: no período, o delegado vê e decide as pendências do titular</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <div className="br-card" style={{ marginBottom: 24 }}>
        <h3 className="br-card__title" style={{ marginBottom: 12 }}>Nova delegação</h3>
        <form onSubmit={criar} noValidate>
          <div className="form-row" style={{ gap: 12 }}>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="del-titular">Gestor titular (delega)</label>
              <select id="del-titular" className="br-select" value={delegante} onChange={(e) => setDelegante(e.target.value)}>
                <option value="">— Selecione —</option>
                {servidores.map((s) => <option key={s.id} value={s.id}>{s.nome}</option>)}
              </select>
            </div>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="del-sub">Substituto (recebe)</label>
              <select id="del-sub" className="br-select" value={delegado} onChange={(e) => setDelegado(e.target.value)}>
                <option value="">— Selecione —</option>
                {servidores.map((s) => <option key={s.id} value={s.id}>{s.nome}</option>)}
              </select>
            </div>
          </div>
          <div className="form-row" style={{ gap: 12 }}>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="del-ini">Início</label>
              <input id="del-ini" type="date" className="br-input" value={dataInicio} onChange={(e) => setDataInicio(e.target.value)} />
            </div>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="del-fim">Fim</label>
              <input id="del-fim" type="date" className="br-input" value={dataFim} onChange={(e) => setDataFim(e.target.value)} />
            </div>
          </div>
          <button className="br-button br-button--primary" type="submit" disabled={saving || !delegante || !delegado || !dataInicio || !dataFim}>
            {saving ? 'Salvando…' : 'Criar delegação'}
          </button>
        </form>
      </div>

      {loading ? <Loading /> : delegacoes.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">🤝</div>
          <p className="empty-state__title">Nenhuma delegação</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Delegações</caption>
            <thead>
              <tr>
                <th scope="col">Titular</th>
                <th scope="col">Substituto</th>
                <th scope="col">Período</th>
                <th scope="col">Situação</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              {delegacoes.map((d) => (
                <tr key={d.id}>
                  <td>{nomeServidor(d.deleganteServidorId)}</td>
                  <td>{nomeServidor(d.delegadoServidorId)}</td>
                  <td>{d.dataInicio} a {d.dataFim}</td>
                  <td>
                    {d.ativo
                      ? <span className="br-badge br-badge--success">Ativa</span>
                      : <span className="br-badge br-badge--neutral">Revogada</span>}
                  </td>
                  <td>
                    {d.ativo && (
                      <button className="br-button br-button--danger br-button--sm" onClick={() => void revogar(d.id)}>Revogar</button>
                    )}
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
