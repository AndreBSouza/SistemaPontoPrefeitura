import { useState } from 'react'
import { useEffect } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type { RelatorioFrequenciaResponse, ServidorResponse, VinculoResponse } from '../api/types'

function minToHHMM(min: number) {
  const h = Math.floor(Math.abs(min) / 60)
  const m = Math.abs(min) % 60
  return `${String(h).padStart(2, '0')}h${String(m).padStart(2, '0')}min`
}

export default function RelatoriosPage() {
  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [vinculoId, setVinculoId] = useState('')
  const [competencia, setCompetencia] = useState(() => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  })
  const [relatorio, setRelatorio] = useState<RelatorioFrequenciaResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [loadingSrvs, setLoadingSrvs] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [csvDownloading, setCsvDownloading] = useState(false)

  useEffect(() => {
    api.get<ServidorResponse[]>('/api/servidores')
      .then(setServidores)
      .catch(setError) // sem isto o seletor fica vazio e parece "nenhum servidor cadastrado"
      .finally(() => setLoadingSrvs(false))
  }, [])

  const vinculos: Array<{ vinculo: VinculoResponse; servidor: ServidorResponse }> = servidores.flatMap((s) =>
    s.vinculos.map((v) => ({ vinculo: v, servidor: s }))
  )

  async function buscar() {
    if (!vinculoId) return
    setLoading(true)
    setError(null)
    try {
      setRelatorio(await api.get<RelatorioFrequenciaResponse>(
        `/api/relatorios/frequencia?vinculoId=${vinculoId}&competencia=${competencia}`
      ))
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  async function downloadCSV() {
    if (!vinculoId) return
    setCsvDownloading(true)
    try {
      const csv = await api.get<string>(
        `/api/relatorios/frequencia/csv?vinculoId=${vinculoId}&competencia=${competencia}`
      )
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `frequencia_${competencia}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setError(e)
    } finally {
      setCsvDownloading(false)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Relatório de Frequência' }]} />
      <h1 className="page-title">Relatório de Frequência</h1>
      <p className="page-subtitle">Resumo mensal de frequência por vínculo</p>

      {error && <ErrorAlert error={error} />}

      {loadingSrvs ? <Loading label="Carregando servidores…" /> : (
        <div className="br-card" style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div className="br-form-group" style={{ marginBottom: 0, flex: 1, minWidth: 240 }}>
              <label className="br-label" htmlFor="vinculo-rel">Vínculo</label>
              <select id="vinculo-rel" className="br-select" value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
                <option value="">— Selecione —</option>
                {vinculos.map(({ vinculo, servidor }) => (
                  <option key={vinculo.id} value={vinculo.id}>
                    {servidor.nome} — {vinculo.matricula}
                  </option>
                ))}
              </select>
            </div>
            <div className="br-form-group" style={{ marginBottom: 0 }}>
              <label className="br-label" htmlFor="comp-rel">Competência</label>
              <input id="comp-rel" type="month" className="br-input" value={competencia} onChange={(e) => setCompetencia(e.target.value)} />
            </div>
            <button className="br-button br-button--primary" onClick={() => void buscar()} disabled={!vinculoId || loading}>
              Gerar
            </button>
            {relatorio && (
              <button className="br-button br-button--secondary" onClick={() => void downloadCSV()} disabled={csvDownloading}>
                {csvDownloading ? 'Baixando…' : '⬇ CSV'}
              </button>
            )}
          </div>
        </div>
      )}

      {loading && <Loading />}

      {relatorio && (
        <div className="stat-grid">
          <div className="stat-card">
            <p className="stat-card__label">Horas trabalhadas</p>
            <p className="stat-card__value">{minToHHMM(relatorio.totalMinutosTrabalhados)}</p>
          </div>
          <div className="stat-card">
            <p className="stat-card__label">Horas esperadas</p>
            <p className="stat-card__value">{minToHHMM(relatorio.totalMinutosEsperados)}</p>
          </div>
          <div className="stat-card">
            <p className="stat-card__label">Horas extras</p>
            <p className="stat-card__value" style={{ color: 'var(--color-success)' }}>
              {minToHHMM(relatorio.minutosHoraExtra)}
            </p>
          </div>
          <div className="stat-card">
            <p className="stat-card__label">Atrasos</p>
            <p className="stat-card__value" style={{ color: 'var(--color-danger)' }}>
              {relatorio.qtdAtrasos}
            </p>
            <p className="stat-card__sub">ocorrências</p>
          </div>
          <div className="stat-card">
            <p className="stat-card__label">Faltas</p>
            <p className="stat-card__value" style={{ color: 'var(--color-danger)' }}>
              {relatorio.qtdFaltas}
            </p>
          </div>
          <div className="stat-card">
            <p className="stat-card__label">Dias justificados</p>
            <p className="stat-card__value">{relatorio.diasJustificados}</p>
          </div>
        </div>
      )}
    </>
  )
}
