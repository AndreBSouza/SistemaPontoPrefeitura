import { useEffect, useState, useRef, FormEvent } from 'react'
import { Modal } from '../../components/Modal'
import { Alert, ErrorAlert } from '../../components/Alert'
import { api } from '../../api/client'
import type { ImportacaoResultado, LotacaoResponse } from '../../api/types'

export function ImportarCSVModal({ onClose, onDone }: {
  onClose: () => void
  onDone: (msg: string) => void
}) {
  const fileRef = useRef<HTMLInputElement>(null)
  const [resultado, setResultado] = useState<ImportacaoResultado | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [lotacoes, setLotacoes] = useState<LotacaoResponse[]>([])
  const [lotacaoId, setLotacaoId] = useState('')

  useEffect(() => {
    api.get<LotacaoResponse[]>('/api/lotacoes').then(setLotacoes).catch(() => { /* opcional */ })
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const file = fileRef.current?.files?.[0]
    if (!file) { setError(new Error('Selecione um arquivo CSV.')); return }
    setSaving(true)
    setError(null)
    try {
      const csv = await file.text()
      const url = lotacaoId
        ? `/api/servidores/importar?lotacaoId=${lotacaoId}`
        : '/api/servidores/importar'
      const res = await api.postText<ImportacaoResultado>(url, csv)
      setResultado(res)
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  if (resultado) {
    return (
      <Modal
        title="Resultado da Importação"
        onClose={() => onDone(`${resultado.importados} servidor(es) importado(s).`)}
        footer={
          <button className="br-button br-button--primary" onClick={() => onDone(`${resultado.importados} servidor(es) importado(s).`)}>
            Fechar
          </button>
        }
      >
        <Alert variant={resultado.erros.length === 0 ? 'success' : 'warning'}>
          <strong>{resultado.importados}</strong> de <strong>{resultado.totalLinhas}</strong> linha(s) importada(s).
        </Alert>
        {resultado.erros.length > 0 && (
          <div>
            <p style={{ fontWeight: 600, marginBottom: 8 }}>Erros por linha:</p>
            <div className="br-table-wrapper">
              <table className="br-table">
                <thead>
                  <tr><th scope="col">Linha</th><th scope="col">Mensagem</th></tr>
                </thead>
                <tbody>
                  {resultado.erros.map((e) => (
                    <tr key={e.linha}>
                      <td>{e.linha}</td>
                      <td>{e.mensagem}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </Modal>
    )
  }

  return (
    <Modal
      title="Importar Servidores (CSV)"
      onClose={onClose}
      footer={
        <>
          <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
          <button
            className="br-button br-button--primary"
            form="form-importar"
            type="submit"
            disabled={saving}
          >
            {saving ? 'Enviando…' : 'Importar'}
          </button>
        </>
      }
    >
      {error != null && <ErrorAlert error={error} />}
      <Alert variant="info">
        Colunas: cpf, nome, email, matricula, regime, cargo, cargaHorariaSemanal, lotacaoSigla (as 2 últimas opcionais).
        A coluna <code>lotacaoSigla</code> lota cada servidor no seu órgão; sem ela, usa o órgão selecionado abaixo.
      </Alert>
      <form id="form-importar" onSubmit={handleSubmit} noValidate>
        <div className="br-form-group">
          <label className="br-label" htmlFor="imp-lotacao">Órgão padrão (opcional)</label>
          <select id="imp-lotacao" className="br-select" value={lotacaoId} onChange={(e) => setLotacaoId(e.target.value)}>
            <option value="">— Não lotar / usar coluna lotacaoSigla —</option>
            {lotacoes.map((l) => <option key={l.id} value={l.id}>{l.nome}</option>)}
          </select>
        </div>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="csv-file">Arquivo CSV</label>
          <input
            id="csv-file"
            ref={fileRef}
            type="file"
            accept=".csv,text/csv,text/plain"
            className="br-input"
            required
          />
        </div>
      </form>
    </Modal>
  )
}
