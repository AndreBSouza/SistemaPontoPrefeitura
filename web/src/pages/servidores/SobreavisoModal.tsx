import { useEffect, useState, FormEvent } from 'react'
import { Modal } from '../../components/Modal'
import { Alert, ErrorAlert } from '../../components/Alert'
import { Loading } from '../../components/Loading'
import { api } from '../../api/client'
import type { SobreavisoResponse, RegistrarSobreavisoRequest } from '../../api/types'

export function SobreavisoModal({ vinculoId, onClose }: {
  vinculoId: string
  onClose: () => void
}) {
  const [itens, setItens] = useState<SobreavisoResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [saving, setSaving] = useState(false)
  const [data, setData] = useState('')
  const [horas, setHoras] = useState('')
  const [obs, setObs] = useState('')

  function load() {
    setLoading(true)
    api
      .get<SobreavisoResponse[]>(`/api/sobreaviso?vinculoId=${vinculoId}`)
      .then(setItens)
      .catch(setError)
      .finally(() => setLoading(false))
  }
  useEffect(load, [vinculoId])

  async function adicionar(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body: RegistrarSobreavisoRequest = {
        vinculoId,
        data,
        minutos: Math.round(Number(horas) * 60),
        observacao: obs || undefined,
      }
      await api.post<SobreavisoResponse>('/api/sobreaviso', body)
      setData(''); setHoras(''); setObs('')
      load()
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function remover(id: string) {
    setError(null)
    try {
      await api.delete<void>(`/api/sobreaviso/${id}`)
      load()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <Modal title="Sobreaviso (on-call)" onClose={onClose}>
      {error != null && <ErrorAlert error={error} />}
      <Alert variant="info">
        Horas em que o servidor fica à disposição fora do expediente. São contadas à parte (não
        afetam a apuração) e somadas por competência na exportação para a folha.
      </Alert>

      <form onSubmit={adicionar} noValidate style={{ marginBottom: 16 }}>
        <div className="form-row-3">
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="sa-data">Data</label>
            <input id="sa-data" type="date" className="br-input" value={data}
                   onChange={(e) => setData(e.target.value)} required />
          </div>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="sa-horas">Horas</label>
            <input id="sa-horas" type="number" min="0.5" step="0.5" className="br-input" value={horas}
                   onChange={(e) => setHoras(e.target.value)} required placeholder="12" />
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="sa-obs">Observação</label>
            <input id="sa-obs" className="br-input" value={obs} onChange={(e) => setObs(e.target.value)} />
          </div>
        </div>
        <button className="br-button br-button--secondary" type="submit" disabled={saving}>
          {saving ? 'Adicionando…' : 'Adicionar sobreaviso'}
        </button>
      </form>

      {loading ? (
        <Loading label="Carregando…" />
      ) : itens.length === 0 ? (
        <div className="empty-state"><p className="empty-state__title">Nenhum sobreaviso registrado</p></div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Sobreavisos do servidor</caption>
            <thead>
              <tr>
                <th scope="col">Data</th>
                <th scope="col">Horas</th>
                <th scope="col">Observação</th>
                <th scope="col">Ação</th>
              </tr>
            </thead>
            <tbody>
              {itens.map((s) => (
                <tr key={s.id}>
                  <td>{s.data}</td>
                  <td>{(s.minutos / 60).toLocaleString('pt-BR')} h</td>
                  <td>{s.observacao ?? '—'}</td>
                  <td>
                    <button className="br-button br-button--sm br-button--danger"
                            onClick={() => void remover(s.id)}
                            aria-label={`Remover sobreaviso de ${s.data}`}>
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  )
}
