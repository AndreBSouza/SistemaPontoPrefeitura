import { useEffect, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type {
  AusenciaResponse,
  AgendarAusenciaRequest,
  CoberturaResponse,
  LotacaoResponse,
  ServidorResponse,
  TipoAusencia,
} from '../api/types'

const TIPO_LABEL: Record<TipoAusencia, string> = {
  FERIAS: 'Férias',
  LICENCA_MEDICA: 'Licença médica',
  LICENCA_MATERNIDADE: 'Licença maternidade',
  LICENCA_PATERNIDADE: 'Licença paternidade',
  LICENCA_PREMIO: 'Licença-prêmio',
  LICENCA_NOJO: 'Licença nojo',
  VIAGEM: 'Viagem a serviço',
  CAPACITACAO: 'Capacitação',
  OUTRA: 'Outra',
}

export default function FeriasPage() {
  const [competencia, setCompetencia] = useState(() => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  })
  const [ausencias, setAusencias] = useState<AusenciaResponse[]>([])
  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [lotacoes, setLotacoes] = useState<LotacaoResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')

  const [vinculoId, setVinculoId] = useState('')
  const [tipo, setTipo] = useState<TipoAusencia>('FERIAS')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [observacao, setObservacao] = useState('')
  const [saving, setSaving] = useState(false)

  const [coberturaLotacao, setCoberturaLotacao] = useState('')
  const [cobertura, setCobertura] = useState<CoberturaResponse | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [a, s, l] = await Promise.all([
        api.get<AusenciaResponse[]>(`/api/ausencias?competencia=${competencia}`),
        api.get<ServidorResponse[]>('/api/servidores'),
        api.get<LotacaoResponse[]>('/api/lotacoes'),
      ])
      setAusencias(a)
      setServidores(s)
      setLotacoes(l)
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [competencia])

  function flash(msg: string) {
    setSucesso(msg)
    setTimeout(() => setSucesso(''), 4000)
  }

  const vinculos = servidores.flatMap((s) => s.vinculos.map((v) => ({ v, s })))
  const nomeVinculo = (id: string) => {
    const f = vinculos.find((x) => x.v.id === id)
    return f ? `${f.s.nome} — ${f.v.matricula}` : id
  }

  async function agendar(e: FormEvent) {
    e.preventDefault()
    if (!vinculoId || !dataInicio || !dataFim) return
    setSaving(true)
    setError(null)
    try {
      const body: AgendarAusenciaRequest = {
        vinculoId,
        tipo,
        dataInicio,
        dataFim,
        observacao: observacao.trim() || undefined,
      }
      await api.post<AusenciaResponse>('/api/ausencias', body)
      flash('Ausência programada.')
      setDataInicio('')
      setDataFim('')
      setObservacao('')
      void load()
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function remover(id: string) {
    try {
      await api.delete<void>(`/api/ausencias/${id}`)
      flash('Ausência removida.')
      void load()
    } catch (err) {
      setError(err)
    }
  }

  async function carregarCobertura() {
    if (!coberturaLotacao) return
    setError(null)
    try {
      setCobertura(await api.get<CoberturaResponse>(`/api/ausencias/cobertura?lotacaoId=${coberturaLotacao}&competencia=${competencia}`))
    } catch (e) {
      setError(e)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Férias e licenças' }]} />
      <h1 className="page-title">Férias e licenças</h1>
      <p className="page-subtitle">Programe ausências (não geram falta na apuração) e acompanhe a cobertura da equipe</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <div className="br-card" style={{ marginBottom: 24 }}>
        <h3 className="br-card__title" style={{ marginBottom: 12 }}>Programar ausência</h3>
        <form onSubmit={agendar} noValidate>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="aus-vinculo">Vínculo</label>
            <select id="aus-vinculo" className="br-select" value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
              <option value="">— Selecione —</option>
              {vinculos.map(({ v, s }) => (
                <option key={v.id} value={v.id}>{s.nome} — {v.matricula}</option>
              ))}
            </select>
          </div>
          <div className="form-row" style={{ gap: 12 }}>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="aus-tipo">Tipo</label>
              <select id="aus-tipo" className="br-select" value={tipo} onChange={(e) => setTipo(e.target.value as TipoAusencia)}>
                {(Object.keys(TIPO_LABEL) as TipoAusencia[]).map((t) => (
                  <option key={t} value={t}>{TIPO_LABEL[t]}</option>
                ))}
              </select>
            </div>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="aus-ini">Início</label>
              <input id="aus-ini" type="date" className="br-input" value={dataInicio} onChange={(e) => setDataInicio(e.target.value)} />
            </div>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="aus-fim">Fim</label>
              <input id="aus-fim" type="date" className="br-input" value={dataFim} onChange={(e) => setDataFim(e.target.value)} />
            </div>
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="aus-obs">Observação</label>
            <input id="aus-obs" className="br-input" value={observacao} onChange={(e) => setObservacao(e.target.value)} maxLength={300} />
          </div>
          <button className="br-button br-button--primary" type="submit" disabled={saving || !vinculoId || !dataInicio || !dataFim}>
            {saving ? 'Salvando…' : 'Programar'}
          </button>
        </form>
      </div>

      <div className="action-bar">
        <div className="action-bar__left">
          <label className="br-label" htmlFor="aus-comp" style={{ marginBottom: 0, marginRight: 8 }}>Competência</label>
          <input id="aus-comp" type="month" className="br-input" style={{ width: 'auto', display: 'inline-block' }} value={competencia} onChange={(e) => setCompetencia(e.target.value)} />
        </div>
        <div className="action-bar__right"><span>{ausencias.length} ausência(s)</span></div>
      </div>

      {loading ? <Loading /> : ausencias.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">🏖️</div>
          <p className="empty-state__title">Nenhuma ausência nesta competência</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Ausências programadas</caption>
            <thead>
              <tr>
                <th scope="col">Vínculo</th>
                <th scope="col">Tipo</th>
                <th scope="col">Período</th>
                <th scope="col">Dias</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              {ausencias.map((a) => (
                <tr key={a.id}>
                  <td>{nomeVinculo(a.vinculoId)}</td>
                  <td><span className="br-badge br-badge--info">{TIPO_LABEL[a.tipo]}</span></td>
                  <td>{a.dataInicio} a {a.dataFim}</td>
                  <td>{a.dias}</td>
                  <td>
                    <button className="br-button br-button--danger br-button--sm" onClick={() => void remover(a.id)}>Remover</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Cobertura da equipe por órgão */}
      <div className="br-card" style={{ marginTop: 32 }}>
        <h3 className="br-card__title" style={{ marginBottom: 12 }}>Cobertura da equipe</h3>
        <div className="form-row" style={{ gap: 12, alignItems: 'flex-end' }}>
          <div className="br-form-group" style={{ marginBottom: 0 }}>
            <label className="br-label" htmlFor="cob-orgao">Órgão</label>
            <select id="cob-orgao" className="br-select" value={coberturaLotacao} onChange={(e) => setCoberturaLotacao(e.target.value)}>
              <option value="">— Selecione —</option>
              {lotacoes.map((l) => (
                <option key={l.id} value={l.id}>{l.nome}</option>
              ))}
            </select>
          </div>
          <button className="br-button br-button--secondary br-button--sm" onClick={() => void carregarCobertura()} disabled={!coberturaLotacao}>
            Ver cobertura
          </button>
        </div>

        {cobertura && (
          <div style={{ marginTop: 12 }}>
            <p style={{ fontSize: 13 }}>
              <strong>{cobertura.ausencias.length}</strong> de <strong>{cobertura.totalVinculos}</strong> vínculo(s) do órgão com ausência na competência.
            </p>
            {cobertura.ausencias.length > 0 && (
              <div className="br-table-wrapper">
                <table className="br-table">
                  <caption className="visually-hidden">Cobertura</caption>
                  <thead>
                    <tr>
                      <th scope="col">Servidor</th>
                      <th scope="col">Tipo</th>
                      <th scope="col">Período</th>
                    </tr>
                  </thead>
                  <tbody>
                    {cobertura.ausencias.map((a, idx) => (
                      <tr key={idx}>
                        <td>{a.servidor}</td>
                        <td>{TIPO_LABEL[a.tipo]}</td>
                        <td>{a.dataInicio} a {a.dataFim}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>
    </>
  )
}
