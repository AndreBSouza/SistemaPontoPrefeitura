import { useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { ErrorAlert } from '../components/Alert'
import { api } from '../api/client'

export default function IntegracoesPage() {
  const [competencia, setCompetencia] = useState(() => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function downloadFolha() {
    setLoading(true)
    setError(null)
    try {
      const csv = await api.get<string>(`/api/integracoes/folha/csv?competencia=${competencia}`)
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `folha_${competencia}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  async function downloadAFD() {
    setLoading(true)
    setError(null)
    try {
      const data = await api.get<{ conteudo: string }>(`/api/relatorios/afd?competencia=${competencia}`)
      const blob = new Blob([data.conteudo], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `AFD_${competencia}.txt`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  async function downloadAEJ() {
    setLoading(true)
    setError(null)
    try {
      const data = await api.get<{ conteudo: string }>(`/api/relatorios/aej?competencia=${competencia}`)
      const blob = new Blob([data.conteudo], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `AEJ_${competencia}.txt`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Integrações' }]} />
      <h1 className="page-title">Integrações — Folha / eSocial</h1>
      <p className="page-subtitle">Exportação de arquivos para integração com sistemas de folha e eSocial</p>

      {error && <ErrorAlert error={error} />}

      <div className="br-card" style={{ marginBottom: 24 }}>
        <div className="br-card__header">
          <h2 className="br-card__title">Competência</h2>
        </div>
        <div className="br-form-group">
          <label className="br-label" htmlFor="comp-int">Mês de referência</label>
          <input
            id="comp-int"
            type="month"
            className="br-input"
            value={competencia}
            onChange={(e) => setCompetencia(e.target.value)}
            style={{ maxWidth: 220 }}
          />
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: 16 }}>
        <div className="br-card">
          <h3 className="br-card__title" style={{ marginBottom: 8 }}>Folha de Pagamento</h3>
          <p style={{ fontSize: 14, color: 'var(--color-gray-60)', marginBottom: 16 }}>
            Exporta totalizadores por vínculo em formato CSV para integração com sistema de folha.
          </p>
          <button
            className="br-button br-button--primary br-button--block"
            onClick={() => void downloadFolha()}
            disabled={loading}
          >
            ⬇ Exportar CSV da Folha
          </button>
        </div>

        <div className="br-card">
          <h3 className="br-card__title" style={{ marginBottom: 8 }}>AFD (Portaria 1510)</h3>
          <p style={{ fontSize: 14, color: 'var(--color-gray-60)', marginBottom: 16 }}>
            Arquivo Fonte de Dados — formato eletrônico de registros de ponto conforme Portaria MTE 1510/2009.
          </p>
          <button
            className="br-button br-button--secondary br-button--block"
            onClick={() => void downloadAFD()}
            disabled={loading}
          >
            ⬇ Exportar AFD
          </button>
        </div>

        <div className="br-card">
          <h3 className="br-card__title" style={{ marginBottom: 8 }}>AEJ (Espelho eletrônico)</h3>
          <p style={{ fontSize: 14, color: 'var(--color-gray-60)', marginBottom: 16 }}>
            Arquivo de Espelho de Jornada — registro eletrônico para fins de conformidade.
          </p>
          <button
            className="br-button br-button--secondary br-button--block"
            onClick={() => void downloadAEJ()}
            disabled={loading}
          >
            ⬇ Exportar AEJ
          </button>
        </div>
      </div>
    </>
  )
}
