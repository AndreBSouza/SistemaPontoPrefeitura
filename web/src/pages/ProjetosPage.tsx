import { useEffect, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type { ServidorResponse } from '../api/types'

interface ProjetoResp { id: string; nome: string; fonte: string | null }
interface RelLinha { projetoId: string; nome: string; fonte: string | null; totalMinutos: number; lancamentos: number }
interface RelResp { competencia: string; projetos: RelLinha[] }

function hhmm(min: number) {
  return `${Math.floor(min / 60)}h ${String(min % 60).padStart(2, '0')}min`
}

export default function ProjetosPage() {
  const [projetos, setProjetos] = useState<ProjetoResp[]>([])
  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')

  const [nome, setNome] = useState('')
  const [fonte, setFonte] = useState('')

  const [vinculoId, setVinculoId] = useState('')
  const [projetoId, setProjetoId] = useState('')
  const [data, setData] = useState('')
  const [horas, setHoras] = useState('')
  const [descricao, setDescricao] = useState('')

  const [competencia, setCompetencia] = useState(() => {
    const n = new Date()
    return `${n.getFullYear()}-${String(n.getMonth() + 1).padStart(2, '0')}`
  })
  const [rel, setRel] = useState<RelResp | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [p, s] = await Promise.all([
        api.get<ProjetoResp[]>('/api/projetos'),
        api.get<ServidorResponse[]>('/api/servidores'),
      ])
      setProjetos(p)
      setServidores(s)
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [])

  function flash(m: string) { setSucesso(m); setTimeout(() => setSucesso(''), 4000) }

  const vinculos = servidores.flatMap((s) => s.vinculos.map((v) => ({ v, s })))

  async function criarProjeto(e: FormEvent) {
    e.preventDefault()
    if (!nome.trim()) return
    try {
      await api.post<ProjetoResp>('/api/projetos', { nome: nome.trim(), fonte: fonte.trim() || undefined })
      flash('Projeto criado.')
      setNome(''); setFonte('')
      void load()
    } catch (e) { setError(e) }
  }

  async function apropriar(e: FormEvent) {
    e.preventDefault()
    if (!vinculoId || !projetoId || !data || !horas) return
    try {
      await api.post<void>('/api/projetos/apropriacoes', {
        vinculoId, projetoId, data, minutos: Math.round(Number(horas) * 60), descricao: descricao.trim() || undefined,
      })
      flash('Horas apropriadas.')
      setHoras(''); setDescricao('')
    } catch (e) { setError(e) }
  }

  async function carregarRel() {
    setError(null)
    try {
      setRel(await api.get<RelResp>(`/api/projetos/relatorio?competencia=${competencia}`))
    } catch (e) { setError(e) }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Projetos / Convênios' }]} />
      <h1 className="page-title">Projetos e convênios</h1>
      <p className="page-subtitle">Apropriação de horas por projeto/fonte de recurso — apoio à prestação de contas</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <div className="br-card" style={{ marginBottom: 24 }}>
        <h3 className="br-card__title" style={{ marginBottom: 12 }}>Novo projeto / convênio</h3>
        <form onSubmit={criarProjeto} noValidate>
          <div className="form-row" style={{ gap: 12 }}>
            <div className="br-form-group"><label className="br-label br-label--required" htmlFor="p-nome">Nome</label><input id="p-nome" className="br-input" value={nome} onChange={(e) => setNome(e.target.value)} /></div>
            <div className="br-form-group"><label className="br-label" htmlFor="p-fonte">Fonte / nº convênio</label><input id="p-fonte" className="br-input" value={fonte} onChange={(e) => setFonte(e.target.value)} /></div>
          </div>
          <button className="br-button br-button--primary" type="submit" disabled={!nome.trim()}>Criar projeto</button>
        </form>
      </div>

      {loading ? <Loading /> : (
        <div className="br-card" style={{ marginBottom: 24 }}>
          <h3 className="br-card__title" style={{ marginBottom: 12 }}>Apropriar horas</h3>
          <form onSubmit={apropriar} noValidate>
            <div className="form-row" style={{ gap: 12 }}>
              <div className="br-form-group"><label className="br-label br-label--required" htmlFor="a-vinc">Vínculo</label>
                <select id="a-vinc" className="br-select" value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
                  <option value="">— Selecione —</option>
                  {vinculos.map(({ v, s }) => <option key={v.id} value={v.id}>{s.nome} — {v.matricula}</option>)}
                </select>
              </div>
              <div className="br-form-group"><label className="br-label br-label--required" htmlFor="a-proj">Projeto</label>
                <select id="a-proj" className="br-select" value={projetoId} onChange={(e) => setProjetoId(e.target.value)}>
                  <option value="">— Selecione —</option>
                  {projetos.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
                </select>
              </div>
            </div>
            <div className="form-row" style={{ gap: 12 }}>
              <div className="br-form-group"><label className="br-label br-label--required" htmlFor="a-data">Data</label><input id="a-data" type="date" className="br-input" value={data} onChange={(e) => setData(e.target.value)} /></div>
              <div className="br-form-group"><label className="br-label br-label--required" htmlFor="a-horas">Horas</label><input id="a-horas" type="number" step="0.5" className="br-input" value={horas} onChange={(e) => setHoras(e.target.value)} /></div>
            </div>
            <div className="br-form-group"><label className="br-label" htmlFor="a-desc">Descrição</label><input id="a-desc" className="br-input" value={descricao} onChange={(e) => setDescricao(e.target.value)} /></div>
            <button className="br-button br-button--primary" type="submit" disabled={!vinculoId || !projetoId || !data || !horas}>Apropriar</button>
          </form>
        </div>
      )}

      <div className="action-bar">
        <div className="action-bar__left">
          <label className="br-label" htmlFor="rel-comp" style={{ marginBottom: 0, marginRight: 8 }}>Competência</label>
          <input id="rel-comp" type="month" className="br-input" style={{ width: 'auto', display: 'inline-block' }} value={competencia} onChange={(e) => setCompetencia(e.target.value)} />
        </div>
        <div className="action-bar__right"><button className="br-button br-button--secondary br-button--sm" onClick={() => void carregarRel()}>Relatório por projeto</button></div>
      </div>

      {rel && (
        rel.projetos.length === 0 ? <p style={{ fontSize: 13, color: 'var(--color-gray-60)' }}>Nenhuma apropriação no período.</p> : (
          <div className="br-table-wrapper">
            <table className="br-table">
              <caption className="visually-hidden">Apropriação por projeto</caption>
              <thead><tr><th scope="col">Projeto</th><th scope="col">Fonte</th><th scope="col">Horas</th><th scope="col">Lançamentos</th></tr></thead>
              <tbody>
                {rel.projetos.map((l) => (
                  <tr key={l.projetoId}><td>{l.nome}</td><td>{l.fonte ?? '—'}</td><td>{hhmm(l.totalMinutos)}</td><td>{l.lancamentos}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
    </>
  )
}
