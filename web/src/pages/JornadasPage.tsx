import { useEffect, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { Modal } from '../components/Modal'
import { api } from '../api/client'
import type { JornadaResponse, CriarJornadaRequest, HorarioResponse, HorarioRequest } from '../api/types'

const TIPOS_JORNADA = ['FIXA', 'FLEXIVEL', 'ESCALA_12X36', 'PLANTAO', 'MAGISTERIO']
// dia_semana segue ISO no backend: 1=Seg ... 7=Dom
const DIAS = [
  { v: 1, l: 'Seg' }, { v: 2, l: 'Ter' }, { v: 3, l: 'Qua' }, { v: 4, l: 'Qui' },
  { v: 5, l: 'Sex' }, { v: 6, l: 'Sáb' }, { v: 7, l: 'Dom' },
]

export default function JornadasPage() {
  const [jornadas, setJornadas] = useState<JornadaResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [showCriar, setShowCriar] = useState(false)
  const [showHorarios, setShowHorarios] = useState<JornadaResponse | null>(null)
  const [sucesso, setSucesso] = useState('')

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setJornadas(await api.get<JornadaResponse[]>('/api/jornadas'))
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [])

  function flash(msg: string) { setSucesso(msg); setTimeout(() => setSucesso(''), 4000) }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Jornadas' }]} />
      <h1 className="page-title">Jornadas de Trabalho</h1>
      <p className="page-subtitle">Configure as jornadas e horários de cada turno</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <div className="action-bar">
        <div className="action-bar__left"><span>{jornadas.length} jornada(s)</span></div>
        <div className="action-bar__right">
          <button className="br-button br-button--primary" onClick={() => setShowCriar(true)}>
            + Nova Jornada
          </button>
        </div>
      </div>

      {loading ? <Loading /> : jornadas.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">🕐</div>
          <p className="empty-state__title">Nenhuma jornada cadastrada</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Lista de jornadas</caption>
            <thead>
              <tr>
                <th scope="col">Nome</th>
                <th scope="col">Tipo</th>
                <th scope="col">CH Semanal (min)</th>
                <th scope="col">Tolerância (min)</th>
                <th scope="col">Intervalo (min)</th>
                <th scope="col">Situação</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              {jornadas.map((j) => (
                <tr key={j.id}>
                  <td>{j.nome}</td>
                  <td><span className="br-badge br-badge--info">{j.tipo}</span></td>
                  <td>{j.cargaHorariaSemanalMin}</td>
                  <td>{j.toleranciaMin}</td>
                  <td>{j.intervaloMin}</td>
                  <td>
                    {j.ativo
                      ? <span className="br-badge br-badge--success">Ativa</span>
                      : <span className="br-badge br-badge--neutral">Inativa</span>}
                  </td>
                  <td>
                    <button
                      className="br-button br-button--secondary br-button--sm"
                      onClick={() => setShowHorarios(j)}
                    >
                      Horários
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCriar && (
        <CriarJornadaModal
          onClose={() => setShowCriar(false)}
          onSaved={(msg) => { setShowCriar(false); void load(); flash(msg) }}
        />
      )}

      {showHorarios && (
        <HorariosModal
          jornada={showHorarios}
          onClose={() => setShowHorarios(null)}
          onSaved={(msg) => { setShowHorarios(null); void load(); flash(msg) }}
        />
      )}
    </>
  )
}

function CriarJornadaModal({ onClose, onSaved }: {
  onClose: () => void
  onSaved: (msg: string) => void
}) {
  const [nome, setNome] = useState('')
  const [tipo, setTipo] = useState('FIXA')
  const [carga, setCarga] = useState('')
  const [tolerancia, setTolerancia] = useState('5')
  const [intervalo, setIntervalo] = useState('60')
  const [horaAtividade, setHoraAtividade] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body: CriarJornadaRequest = {
        nome: nome.trim(),
        tipo,
        cargaHorariaSemanalMin: Number(carga),
        toleranciaMin: Number(tolerancia),
        intervaloMin: Number(intervalo),
        horaAtividadeMin: horaAtividade ? Number(horaAtividade) : null,
      }
      await api.post<JornadaResponse>('/api/jornadas', body)
      onSaved('Jornada criada com sucesso.')
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title="Nova Jornada"
      onClose={onClose}
      footer={
        <>
          <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
          <button className="br-button br-button--primary" form="form-jornada" type="submit" disabled={saving}>
            {saving ? 'Salvando…' : 'Criar'}
          </button>
        </>
      }
    >
      {error != null && <ErrorAlert error={error} />}
      <form id="form-jornada" onSubmit={handleSubmit} noValidate>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="j-nome">Nome</label>
          <input id="j-nome" className="br-input" value={nome} onChange={(e) => setNome(e.target.value)} required autoFocus />
        </div>
        <div className="form-row">
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="j-tipo">Tipo</label>
            <select id="j-tipo" className="br-select" value={tipo} onChange={(e) => setTipo(e.target.value)}>
              {TIPOS_JORNADA.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
            {tipo === 'ESCALA_12X36' && (
              <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '4px 0 0' }}>
                12×36: defina <strong>um único horário</strong> (o turno). Ao criar a escala, a
                <strong> data de início é o 1º dia de trabalho</strong> e a folga alterna a cada dia.
              </p>
            )}
          </div>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="j-carga">CH Semanal (min)</label>
            <input id="j-carga" type="number" min="1" className="br-input" value={carga} onChange={(e) => setCarga(e.target.value)} required />
          </div>
        </div>
        <div className="form-row">
          <div className="br-form-group">
            <label className="br-label" htmlFor="j-tol">Tolerância (min)</label>
            <input id="j-tol" type="number" min="0" className="br-input" value={tolerancia} onChange={(e) => setTolerancia(e.target.value)} />
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="j-int">Intervalo (min)</label>
            <input id="j-int" type="number" min="0" className="br-input" value={intervalo} onChange={(e) => setIntervalo(e.target.value)} />
          </div>
        </div>
        <div className="br-form-group">
          <label className="br-label" htmlFor="j-ha">Hora-atividade semanal (min){tipo === 'MAGISTERIO' ? ' — Lei do Piso' : ' (opcional)'}</label>
          <input id="j-ha" type="number" min="0" className="br-input" value={horaAtividade} onChange={(e) => setHoraAtividade(e.target.value)} placeholder="ex.: 800 (1/3 de 2400)" />
          <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '4px 0 0' }}>
            Parte da carga dedicada a planejamento (fora de sala). O mínimo legal do magistério é 1/3 da carga; a conformidade é verificada em Conformidade.
          </p>
        </div>
      </form>
    </Modal>
  )
}

function HorariosModal({ jornada, onClose, onSaved }: {
  jornada: JornadaResponse
  onClose: () => void
  onSaved: (msg: string) => void
}) {
  const [horarios, setHorarios] = useState<HorarioResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [rows, setRows] = useState<HorarioRequest[]>(
    [1, 2, 3, 4, 5].map((d) => ({ diaSemana: d, horaEntrada: '08:00', horaSaida: '17:00' }))
  )
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [falhouCarregar, setFalhouCarregar] = useState(false)

  useEffect(() => {
    api.get<HorarioResponse[]>(`/api/jornadas/${jornada.id}/horarios`)
      .then((h) => {
        setHorarios(h)
        if (h.length > 0) {
          setRows(h.map((hh) => ({
            diaSemana: hh.diaSemana,
            horaEntrada: hh.horaEntrada,
            horaSaida: hh.horaSaida,
          })))
        }
      })
      // Se a leitura falhar, `rows` fica com o padrão seg–sex 08:00–17:00. Salvar nesse estado
      // SOBRESCREVERIA os horários reais da jornada — então mostra o erro e bloqueia o salvamento.
      .catch((err) => { setError(err); setFalhouCarregar(true) })
      .finally(() => setLoading(false))
  }, [jornada.id])

  function updateRow(idx: number, field: keyof HorarioRequest, value: string | number) {
    setRows((prev) => prev.map((r, i) => i === idx ? { ...r, [field]: value } : r))
  }

  function addRow() {
    setRows((prev) => [...prev, { diaSemana: 1, horaEntrada: '08:00', horaSaida: '17:00' }])
  }

  function removeRow(idx: number) {
    setRows((prev) => prev.filter((_, i) => i !== idx))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await api.put<HorarioResponse[]>(`/api/jornadas/${jornada.id}/horarios`, rows)
      onSaved(`Horários de "${jornada.nome}" salvos.`)
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={`Horários — ${jornada.nome}`}
      onClose={onClose}
      footer={
        <>
          <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
          <button className="br-button br-button--primary" form="form-horarios" type="submit"
            disabled={saving || falhouCarregar}
            title={falhouCarregar ? 'Não foi possível carregar os horários atuais — reabra o modal' : undefined}>
            {saving ? 'Salvando…' : 'Salvar'}
          </button>
        </>
      }
    >
      {error != null && <ErrorAlert error={error} />}
      {loading ? <Loading /> : (
        <>
          {horarios.length > 0 && (
            <Alert variant="info">
              Horários atuais carregados. Edite e salve para atualizar.
            </Alert>
          )}
          <form id="form-horarios" onSubmit={handleSubmit} noValidate>
            {rows.map((row, idx) => (
              <div key={idx} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr auto', gap: 8, marginBottom: 8, alignItems: 'flex-end' }}>
                <div>
                  {idx === 0 && <label className="br-label">Dia</label>}
                  <select
                    className="br-select"
                    value={row.diaSemana}
                    onChange={(e) => updateRow(idx, 'diaSemana', Number(e.target.value))}
                    aria-label={`Dia da semana linha ${idx + 1}`}
                  >
                    {DIAS.map((d) => <option key={d.v} value={d.v}>{d.l}</option>)}
                  </select>
                </div>
                <div>
                  {idx === 0 && <label className="br-label">Entrada</label>}
                  <input
                    type="time"
                    className="br-input"
                    value={row.horaEntrada}
                    onChange={(e) => updateRow(idx, 'horaEntrada', e.target.value)}
                    aria-label={`Hora de entrada linha ${idx + 1}`}
                  />
                </div>
                <div>
                  {idx === 0 && <label className="br-label">Saída</label>}
                  <input
                    type="time"
                    className="br-input"
                    value={row.horaSaida}
                    onChange={(e) => updateRow(idx, 'horaSaida', e.target.value)}
                    aria-label={`Hora de saída linha ${idx + 1}`}
                  />
                </div>
                <div style={{ paddingBottom: 0 }}>
                  {idx === 0 && <div style={{ height: 24 }} />}
                  <button
                    type="button"
                    className="br-button br-button--danger br-button--sm"
                    onClick={() => removeRow(idx)}
                    aria-label={`Remover linha ${idx + 1}`}
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
            <button type="button" className="br-button br-button--tertiary br-button--sm" onClick={addRow}>
              + Adicionar linha
            </button>
          </form>
        </>
      )}
    </Modal>
  )
}
