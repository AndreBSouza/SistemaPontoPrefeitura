import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { Alert, ErrorAlert } from '../../components/Alert'
import { api } from '../../api/client'
import type { VinculoResponse, LotacaoResponse } from '../../api/types'

export function LotarModal({ vinculo, lotacoes, onClose, onSaved }: {
  vinculo: VinculoResponse
  lotacoes: LotacaoResponse[]
  onClose: () => void
  onSaved: (msg: string) => void
}) {
  const [lotacaoId, setLotacaoId] = useState(vinculo.lotacaoId ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function salvar() {
    if (!lotacaoId) { setError(new Error('Selecione um órgão.')); return }
    setSaving(true)
    setError(null)
    try {
      await api.put<void>(`/api/servidores/vinculos/${vinculo.id}/lotacao?lotacaoId=${lotacaoId}`)
      onSaved(`Vínculo ${vinculo.matricula} lotado no órgão.`)
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={`Lotar ${vinculo.matricula} em um órgão`}
      onClose={onClose}
      footer={
        <>
          <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
          <button className="br-button br-button--primary" onClick={() => void salvar()} disabled={saving}>
            {saving ? 'Salvando…' : 'Lotar'}
          </button>
        </>
      }
    >
      {error != null && <ErrorAlert error={error} />}
      <Alert variant="info">
        Vincula o servidor a um órgão/lotação. As regras de ponto e a geofence do órgão passam a valer,
        e o BI por secretaria + a cobertura da equipe passam a agrupá-lo.
      </Alert>
      <div className="br-form-group">
        <label className="br-label br-label--required" htmlFor="lotar-select">Órgão</label>
        <select
          id="lotar-select"
          className="br-select"
          value={lotacaoId}
          onChange={(e) => setLotacaoId(e.target.value)}
        >
          <option value="">— Selecione —</option>
          {lotacoes.map((l) => (
            <option key={l.id} value={l.id}>{l.nome}{l.sigla ? ` (${l.sigla})` : ''}</option>
          ))}
        </select>
      </div>
    </Modal>
  )
}
