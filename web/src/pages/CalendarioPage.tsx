import { useEffect, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type {
  EventoCalendarioResponse,
  CriarEventoRequest,
  LotacaoResponse,
  TipoEventoCalendario,
} from '../api/types'

const TIPO_LABEL: Record<TipoEventoCalendario, string> = {
  FERIADO: 'Feriado',
  PONTO_FACULTATIVO: 'Ponto facultativo',
  ABONO_COLETIVO: 'Abono coletivo',
}

export default function CalendarioPage() {
  const [competencia, setCompetencia] = useState(() => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  })
  const [eventos, setEventos] = useState<EventoCalendarioResponse[]>([])
  const [lotacoes, setLotacoes] = useState<LotacaoResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')

  const [data, setData] = useState('')
  const [tipo, setTipo] = useState<TipoEventoCalendario>('FERIADO')
  const [descricao, setDescricao] = useState('')
  const [lotacaoId, setLotacaoId] = useState('')
  const [saving, setSaving] = useState(false)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [e, l] = await Promise.all([
        api.get<EventoCalendarioResponse[]>(`/api/calendario?competencia=${competencia}`),
        api.get<LotacaoResponse[]>('/api/lotacoes'),
      ])
      setEventos(e)
      setLotacoes(l)
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [competencia])

  function flash(msg: string) {
    setSucesso(msg)
    setTimeout(() => setSucesso(''), 4000)
  }

  function orgaoNome(id: string | null): string {
    if (!id) return 'Todo o ente'
    return lotacoes.find((l) => l.id === id)?.nome ?? id
  }

  async function criar(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body: CriarEventoRequest = {
        data,
        tipo,
        descricao: descricao.trim(),
        lotacaoId: lotacaoId || null,
      }
      await api.post<EventoCalendarioResponse>('/api/calendario', body)
      flash('Evento adicionado ao calendário.')
      setData('')
      setDescricao('')
      setLotacaoId('')
      void load()
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function remover(id: string) {
    try {
      await api.delete<void>(`/api/calendario/${id}`)
      flash('Evento removido.')
      void load()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Calendário oficial' }]} />
      <h1 className="page-title">Calendário oficial</h1>
      <p className="page-subtitle">
        Feriados, pontos facultativos e abonos coletivos — aplicados automaticamente na apuração (não geram falta)
      </p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <div className="br-card" style={{ marginBottom: 24 }}>
        <h3 className="br-card__title" style={{ marginBottom: 12 }}>Novo evento</h3>
        <form onSubmit={criar} noValidate>
          <div className="form-row" style={{ gap: 12 }}>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="ev-data">Data</label>
              <input id="ev-data" type="date" className="br-input" value={data} onChange={(e) => setData(e.target.value)} required />
            </div>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="ev-tipo">Tipo</label>
              <select id="ev-tipo" className="br-select" value={tipo} onChange={(e) => setTipo(e.target.value as TipoEventoCalendario)}>
                <option value="FERIADO">Feriado</option>
                <option value="PONTO_FACULTATIVO">Ponto facultativo</option>
                <option value="ABONO_COLETIVO">Abono coletivo</option>
              </select>
            </div>
          </div>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="ev-desc">Descrição</label>
            <input id="ev-desc" className="br-input" value={descricao} onChange={(e) => setDescricao(e.target.value)} maxLength={200} required />
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="ev-orgao">Abrangência</label>
            <select id="ev-orgao" className="br-select" value={lotacaoId} onChange={(e) => setLotacaoId(e.target.value)}>
              <option value="">Todo o ente</option>
              {lotacoes.map((l) => (
                <option key={l.id} value={l.id}>{l.nome}</option>
              ))}
            </select>
          </div>
          <button className="br-button br-button--primary" type="submit" disabled={saving || !data || !descricao.trim()}>
            {saving ? 'Adicionando…' : 'Adicionar evento'}
          </button>
        </form>
      </div>

      <div className="action-bar">
        <div className="action-bar__left">
          <label className="br-label" htmlFor="cal-comp" style={{ marginBottom: 0, marginRight: 8 }}>Competência</label>
          <input id="cal-comp" type="month" className="br-input" style={{ width: 'auto', display: 'inline-block' }} value={competencia} onChange={(e) => setCompetencia(e.target.value)} />
        </div>
        <div className="action-bar__right"><span>{eventos.length} evento(s)</span></div>
      </div>

      {loading ? <Loading /> : eventos.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">📅</div>
          <p className="empty-state__title">Nenhum evento nesta competência</p>
          <p>Adicione feriados, pontos facultativos ou abonos coletivos acima.</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Eventos do calendário</caption>
            <thead>
              <tr>
                <th scope="col">Data</th>
                <th scope="col">Tipo</th>
                <th scope="col">Descrição</th>
                <th scope="col">Abrangência</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              {eventos.map((ev) => (
                <tr key={ev.id}>
                  <td>{ev.data}</td>
                  <td><span className="br-badge br-badge--info">{TIPO_LABEL[ev.tipo]}</span></td>
                  <td>{ev.descricao}</td>
                  <td>{ev.geral ? 'Todo o ente' : orgaoNome(ev.lotacaoId)}</td>
                  <td>
                    <button className="br-button br-button--danger br-button--sm" onClick={() => void remover(ev.id)}>
                      Remover
                    </button>
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
