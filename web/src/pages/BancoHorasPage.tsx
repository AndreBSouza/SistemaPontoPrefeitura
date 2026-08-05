import { useEffect, useRef, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { Modal } from '../components/Modal'
import { api } from '../api/client'
import type { SaldoResponse, ServidorResponse, VinculoResponse } from '../api/types'

function minToHHMM(min: number) {
  const h = Math.floor(Math.abs(min) / 60)
  const m = Math.abs(min) % 60
  return `${min < 0 ? '-' : ''}${String(h).padStart(2, '0')}h${String(m).padStart(2, '0')}min`
}

export default function BancoHorasPage() {
  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [vinculoId, setVinculoId] = useState('')
  const [saldo, setSaldo] = useState<SaldoResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [loadingSrvs, setLoadingSrvs] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')
  const [showAjuste, setShowAjuste] = useState(false)
  const [showCompensar, setShowCompensar] = useState(false)

  useEffect(() => {
    api.get<ServidorResponse[]>('/api/servidores')
      .then(setServidores)
      .catch(setError) // sem isto o seletor fica vazio e parece "nenhum servidor cadastrado"
      .finally(() => setLoadingSrvs(false))
  }, [])

  const vinculos: Array<{ vinculo: VinculoResponse; servidor: ServidorResponse }> = servidores.flatMap((s) =>
    s.vinculos.map((v) => ({ vinculo: v, servidor: s }))
  )

  /** Identifica a busca de saldo corrente (ignora respostas fora de ordem). */
  const pedidoRef = useRef(0)

  async function buscarSaldo(id: string) {
    if (!id) { setSaldo(null); return }
    // Descarta resposta obsoleta: ao trocar de vínculo rápido, a resposta lenta do ANTERIOR podia
    // chegar depois e sobrescrever a tela — o admin veria o saldo de outro servidor no seletor.
    const meuPedido = ++pedidoRef.current
    setLoading(true)
    setError(null)
    try {
      const resposta = await api.get<SaldoResponse>(`/api/banco-horas/saldo?vinculoId=${id}`)
      if (pedidoRef.current !== meuPedido) return
      setSaldo(resposta)
    } catch (e) {
      if (pedidoRef.current !== meuPedido) return
      setError(e)
    } finally {
      if (pedidoRef.current === meuPedido) setLoading(false)
    }
  }

  function flash(msg: string) { setSucesso(msg); setTimeout(() => setSucesso(''), 4000) }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Banco de Horas' }]} />
      <h1 className="page-title">Banco de Horas</h1>
      <p className="page-subtitle">Consulte saldo e realize ajustes/compensações</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      {loadingSrvs ? <Loading label="Carregando servidores…" /> : (
        <div className="br-card" style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
            <div className="br-form-group" style={{ marginBottom: 0, flex: 1 }}>
              <label className="br-label" htmlFor="vinculo-bh">Vínculo</label>
              <select
                id="vinculo-bh"
                className="br-select"
                value={vinculoId}
                onChange={(e) => { setVinculoId(e.target.value); void buscarSaldo(e.target.value) }}
              >
                <option value="">— Selecione —</option>
                {vinculos.map(({ vinculo, servidor }) => (
                  <option key={vinculo.id} value={vinculo.id}>
                    {servidor.nome} — {vinculo.matricula}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      )}

      {loading && <Loading />}

      {saldo && (
        <>
          <div className="stat-grid" style={{ marginBottom: 24 }}>
            <div className="stat-card">
              <p className="stat-card__label">Saldo atual</p>
              <p
                className="stat-card__value"
                style={{ color: saldo.saldoMinutos >= 0 ? 'var(--color-success)' : 'var(--color-danger)' }}
              >
                {minToHHMM(saldo.saldoMinutos)}
              </p>
              <p className="stat-card__sub">{saldo.saldoMinutos} minutos</p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10 }}>
            <button className="br-button br-button--primary" onClick={() => setShowAjuste(true)}>
              Ajuste manual
            </button>
            <button className="br-button br-button--secondary" onClick={() => setShowCompensar(true)}>
              Compensar horas
            </button>
          </div>
        </>
      )}

      {showAjuste && vinculoId && (
        <AjusteModal
          vinculoId={vinculoId}
          onClose={() => setShowAjuste(false)}
          onSaved={(msg) => { setShowAjuste(false); void buscarSaldo(vinculoId); flash(msg) }}
        />
      )}

      {showCompensar && vinculoId && (
        <CompensarModal
          vinculoId={vinculoId}
          onClose={() => setShowCompensar(false)}
          onSaved={(msg) => { setShowCompensar(false); void buscarSaldo(vinculoId); flash(msg) }}
        />
      )}
    </>
  )
}

function AjusteModal({ vinculoId, onClose, onSaved }: {
  vinculoId: string
  onClose: () => void
  onSaved: (msg: string) => void
}) {
  const [data, setData] = useState(new Date().toISOString().slice(0, 10))
  const [minutos, setMinutos] = useState('')
  const [descricao, setDescricao] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await api.post<unknown>('/api/banco-horas/ajuste', { vinculoId, data, minutos: Number(minutos), descricao })
      onSaved('Ajuste realizado com sucesso.')
    } catch (err) { setError(err) }
    finally { setSaving(false) }
  }

  return (
    <Modal title="Ajuste Manual de Banco de Horas" onClose={onClose} footer={
      <>
        <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
        <button className="br-button br-button--primary" form="form-ajuste" type="submit" disabled={saving}>
          {saving ? 'Salvando…' : 'Salvar'}
        </button>
      </>
    }>
      {error != null && <ErrorAlert error={error} />}
      <form id="form-ajuste" onSubmit={handleSubmit} noValidate>
        <div className="form-row">
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="aj-data">Data</label>
            <input id="aj-data" type="date" className="br-input" value={data} onChange={(e) => setData(e.target.value)} required />
          </div>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="aj-min">Minutos (positivo = crédito, negativo = débito)</label>
            <input id="aj-min" type="number" className="br-input" value={minutos} onChange={(e) => setMinutos(e.target.value)} required placeholder="ex: 60 ou -30" />
          </div>
        </div>
        <div className="br-form-group">
          <label className="br-label" htmlFor="aj-desc">Descrição</label>
          <input id="aj-desc" className="br-input" value={descricao} onChange={(e) => setDescricao(e.target.value)} />
        </div>
      </form>
    </Modal>
  )
}

function CompensarModal({ vinculoId, onClose, onSaved }: {
  vinculoId: string
  onClose: () => void
  onSaved: (msg: string) => void
}) {
  const [data, setData] = useState(new Date().toISOString().slice(0, 10))
  const [minutos, setMinutos] = useState('')
  const [descricao, setDescricao] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await api.post<unknown>('/api/banco-horas/compensar', { vinculoId, data, minutos: Number(minutos), descricao })
      onSaved('Compensação registrada.')
    } catch (err) { setError(err) }
    finally { setSaving(false) }
  }

  return (
    <Modal title="Compensar Horas" onClose={onClose} footer={
      <>
        <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
        <button className="br-button br-button--primary" form="form-compensar" type="submit" disabled={saving}>
          {saving ? 'Salvando…' : 'Registrar'}
        </button>
      </>
    }>
      {error != null && <ErrorAlert error={error} />}
      <form id="form-compensar" onSubmit={handleSubmit} noValidate>
        <div className="form-row">
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="comp-data">Data</label>
            <input id="comp-data" type="date" className="br-input" value={data} onChange={(e) => setData(e.target.value)} required />
          </div>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="comp-min">Minutos a compensar</label>
            <input id="comp-min" type="number" min="1" className="br-input" value={minutos} onChange={(e) => setMinutos(e.target.value)} required />
          </div>
        </div>
        <div className="br-form-group">
          <label className="br-label" htmlFor="comp-desc">Descrição</label>
          <input id="comp-desc" className="br-input" value={descricao} onChange={(e) => setDescricao(e.target.value)} />
        </div>
      </form>
    </Modal>
  )
}
