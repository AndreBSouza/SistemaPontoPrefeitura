import { useEffect, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { Modal } from '../components/Modal'
import { api } from '../api/client'
import type {
  EscalaResponse,
  CriarEscalaRequest,
  ServidorResponse,
  VinculoResponse,
  JornadaResponse,
} from '../api/types'

export default function EscalasPage() {
  const [escalas, setEscalas] = useState<EscalaResponse[]>([])
  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [jornadas, setJornadas] = useState<JornadaResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [filterVinculo, setFilterVinculo] = useState('')
  const [showCriar, setShowCriar] = useState(false)
  const [sucesso, setSucesso] = useState('')

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [srvs, jorns] = await Promise.all([
        api.get<ServidorResponse[]>('/api/servidores'),
        api.get<JornadaResponse[]>('/api/jornadas'),
      ])
      setServidores(srvs)
      setJornadas(jorns)
      setEscalas([])
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  async function buscarEscalas(vinculoId: string) {
    if (!vinculoId) { setEscalas([]); return }
    try {
      setEscalas(await api.get<EscalaResponse[]>(`/api/escalas?vinculoId=${vinculoId}`))
    } catch (e) {
      setError(e)
    }
  }

  useEffect(() => { void load() }, [])

  function flash(msg: string) { setSucesso(msg); setTimeout(() => setSucesso(''), 4000) }

  // Flattened: servidor → vínculo
  const vinculos: Array<{ vinculo: VinculoResponse; servidor: ServidorResponse }> = servidores.flatMap((s) =>
    s.vinculos.map((v) => ({ vinculo: v, servidor: s }))
  )

  function findJornada(id: string) {
    return jornadas.find((j) => j.id === id)
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Escalas' }]} />
      <h1 className="page-title">Escalas</h1>
      <p className="page-subtitle">Associação de vínculo × jornada com vigência</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <div className="action-bar">
        <div className="action-bar__left" style={{ flex: 1 }}>
          <div style={{ width: 340 }}>
            <label className="br-label" htmlFor="filtro-vinculo">Filtrar por vínculo</label>
            <select
              id="filtro-vinculo"
              className="br-select"
              value={filterVinculo}
              onChange={(e) => { setFilterVinculo(e.target.value); void buscarEscalas(e.target.value) }}
            >
              <option value="">— Selecione um vínculo —</option>
              {vinculos.map(({ vinculo, servidor }) => (
                <option key={vinculo.id} value={vinculo.id}>
                  {servidor.nome} — {vinculo.matricula} ({vinculo.regime})
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="action-bar__right">
          <button className="br-button br-button--primary" onClick={() => setShowCriar(true)}>
            + Nova Escala
          </button>
        </div>
      </div>

      {loading ? <Loading /> : !filterVinculo ? (
        <Alert variant="info">Selecione um vínculo para visualizar as escalas.</Alert>
      ) : escalas.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">📅</div>
          <p className="empty-state__title">Nenhuma escala para este vínculo</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Escalas do vínculo</caption>
            <thead>
              <tr>
                <th scope="col">Jornada</th>
                <th scope="col">Início</th>
                <th scope="col">Fim</th>
              </tr>
            </thead>
            <tbody>
              {escalas.map((e) => (
                <tr key={e.id}>
                  <td>{findJornada(e.jornadaId)?.nome ?? e.jornadaId}</td>
                  <td>{e.dataInicio}</td>
                  <td>{e.dataFim ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCriar && (
        <CriarEscalaModal
          vinculos={vinculos}
          jornadas={jornadas}
          onClose={() => setShowCriar(false)}
          onSaved={(msg) => {
            setShowCriar(false)
            if (filterVinculo) void buscarEscalas(filterVinculo)
            flash(msg)
          }}
        />
      )}
    </>
  )
}

function CriarEscalaModal({ vinculos, jornadas, onClose, onSaved }: {
  vinculos: Array<{ vinculo: VinculoResponse; servidor: ServidorResponse }>
  jornadas: JornadaResponse[]
  onClose: () => void
  onSaved: (msg: string) => void
}) {
  const [vinculoId, setVinculoId] = useState('')
  const [jornadaId, setJornadaId] = useState('')
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body: CriarEscalaRequest = {
        vinculoId,
        jornadaId,
        dataInicio,
        dataFim: dataFim || undefined,
      }
      await api.post<EscalaResponse>('/api/escalas', body)
      onSaved('Escala criada com sucesso.')
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title="Nova Escala"
      onClose={onClose}
      footer={
        <>
          <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
          <button className="br-button br-button--primary" form="form-escala" type="submit" disabled={saving}>
            {saving ? 'Salvando…' : 'Criar'}
          </button>
        </>
      }
    >
      {error != null && <ErrorAlert error={error} />}
      <form id="form-escala" onSubmit={handleSubmit} noValidate>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="e-vinculo">Vínculo</label>
          <select id="e-vinculo" className="br-select" value={vinculoId} onChange={(e) => setVinculoId(e.target.value)} required>
            <option value="">— Selecione —</option>
            {vinculos.map(({ vinculo, servidor }) => (
              <option key={vinculo.id} value={vinculo.id}>
                {servidor.nome} — {vinculo.matricula}
              </option>
            ))}
          </select>
        </div>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="e-jornada">Jornada</label>
          <select id="e-jornada" className="br-select" value={jornadaId} onChange={(e) => setJornadaId(e.target.value)} required>
            <option value="">— Selecione —</option>
            {jornadas.map((j) => <option key={j.id} value={j.id}>{j.nome}</option>)}
          </select>
        </div>
        <div className="form-row">
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="e-inicio">Data início</label>
            <input id="e-inicio" type="date" className="br-input" value={dataInicio} onChange={(e) => setDataInicio(e.target.value)} required />
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="e-fim">Data fim (opcional)</label>
            <input id="e-fim" type="date" className="br-input" value={dataFim} onChange={(e) => setDataFim(e.target.value)} />
          </div>
        </div>
      </form>
    </Modal>
  )
}
