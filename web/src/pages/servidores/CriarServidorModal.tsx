import { useState, FormEvent } from 'react'
import { Modal } from '../../components/Modal'
import { ErrorAlert } from '../../components/Alert'
import { api } from '../../api/client'
import type {
  ServidorResponse,
  CriarServidorRequest,
  CriarVinculoRequest,
  LotacaoResponse,
} from '../../api/types'

const REGIMES = ['ESTATUTARIO', 'CELETISTA', 'COMISSIONADO', 'ESTAGIARIO', 'TEMPORARIO', 'TERCEIRIZADO']

export function CriarServidorModal({ onClose, onSaved, lotacoes }: {
  onClose: () => void
  onSaved: (msg: string) => void
  lotacoes: LotacaoResponse[]
}) {
  const [cpf, setCpf] = useState('')
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [matricula, setMatricula] = useState('')
  const [regime, setRegime] = useState('ESTATUTARIO')
  const [cargo, setCargo] = useState('')
  const [cargaHoraria, setCargaHoraria] = useState('')
  const [lotacaoId, setLotacaoId] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const vinculo: CriarVinculoRequest = {
        matricula: matricula.trim(),
        regime,
        cargo: cargo.trim() || undefined,
        cargaHorariaSemanal: cargaHoraria ? Number(cargaHoraria) : undefined,
        lotacaoId: lotacaoId || undefined,
      }
      const body: CriarServidorRequest = {
        cpf: cpf.trim().replace(/\D/g, ''),
        nome: nome.trim(),
        email: email.trim() || undefined,
        vinculos: [vinculo],
      }
      await api.post<ServidorResponse>('/api/servidores', body)
      onSaved('Servidor criado com sucesso.')
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title="Novo Servidor"
      onClose={onClose}
      footer={
        <>
          <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
          <button
            className="br-button br-button--primary"
            form="form-criar-servidor"
            type="submit"
            disabled={saving}
          >
            {saving ? 'Salvando…' : 'Criar'}
          </button>
        </>
      }
    >
      {error != null && <ErrorAlert error={error} />}
      <form id="form-criar-servidor" onSubmit={handleSubmit} noValidate>
        <div className="form-row">
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="srv-cpf">CPF</label>
            <input
              id="srv-cpf"
              className="br-input"
              value={cpf}
              onChange={(e) => setCpf(e.target.value)}
              placeholder="000.000.000-00"
              required
              autoFocus
            />
          </div>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="srv-nome">Nome completo</label>
            <input
              id="srv-nome"
              className="br-input"
              value={nome}
              onChange={(e) => setNome(e.target.value)}
              required
            />
          </div>
        </div>

        <div className="br-form-group">
          <label className="br-label" htmlFor="srv-email">E-mail</label>
          <input
            id="srv-email"
            type="email"
            className="br-input"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
        </div>

        <fieldset style={{ border: '1px solid var(--color-gray-8)', borderRadius: 4, padding: '12px 16px', marginTop: 8 }}>
          <legend style={{ fontSize: 13, fontWeight: 600, padding: '0 4px' }}>Vínculo</legend>
          <div className="form-row">
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="srv-matricula">Matrícula</label>
              <input
                id="srv-matricula"
                className="br-input"
                value={matricula}
                onChange={(e) => setMatricula(e.target.value)}
                required
              />
            </div>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="srv-regime">Regime</label>
              <select
                id="srv-regime"
                className="br-select"
                value={regime}
                onChange={(e) => setRegime(e.target.value)}
                required
              >
                {REGIMES.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </div>
          </div>
          <div className="form-row">
            <div className="br-form-group">
              <label className="br-label" htmlFor="srv-cargo">Cargo</label>
              <input
                id="srv-cargo"
                className="br-input"
                value={cargo}
                onChange={(e) => setCargo(e.target.value)}
              />
            </div>
            <div className="br-form-group">
              <label className="br-label" htmlFor="srv-carga">Carga horária semanal</label>
              <input
                id="srv-carga"
                type="number"
                min="1"
                className="br-input"
                value={cargaHoraria}
                onChange={(e) => setCargaHoraria(e.target.value)}
                placeholder="40"
              />
            </div>
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="srv-lotacao">Órgão / Lotação</label>
            <select
              id="srv-lotacao"
              className="br-select"
              value={lotacaoId}
              onChange={(e) => setLotacaoId(e.target.value)}
            >
              <option value="">— Sem órgão (definir depois) —</option>
              {lotacoes.map((l) => (
                <option key={l.id} value={l.id}>{l.nome}{l.sigla ? ` (${l.sigla})` : ''}</option>
              ))}
            </select>
          </div>
        </fieldset>
      </form>
    </Modal>
  )
}
