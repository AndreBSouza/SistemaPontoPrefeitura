import { useState } from 'react'
import { Breadcrumb } from './components/Breadcrumb'
import { useTenant } from './contexts/TenantContext'
import { api, ApiError } from './api/client'

interface Comprovante {
  nsr: number
  tipo: string
  dataHoraServidor: string
}

const TIPOS: Array<{ key: string; label: string; color: string }> = [
  { key: 'ENTRADA', label: '▶ Entrada', color: 'var(--color-success)' },
  { key: 'SAIDA', label: '⏹ Saída', color: 'var(--color-danger)' },
  { key: 'INTERVALO_INICIO', label: '⏸ Início Intervalo', color: 'var(--color-warning)' },
  { key: 'INTERVALO_FIM', label: '▶ Fim Intervalo', color: 'var(--color-info)' },
]

/** Totem/quiosque de registro de ponto. Pode ser usado stand-alone ou dentro do AppShell. */
export default function TotemPage() {
  const { tenantId } = useTenant()
  const [vinculo, setVinculo] = useState('')
  const [info, setInfo] = useState<string | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function registrar(tipo: string) {
    const vinculoTrimmed = vinculo.trim()
    if (!vinculoTrimmed) {
      setErro('Informe a matrícula ou ID do vínculo.')
      return
    }
    setEnviando(true)
    setErro(null)
    setInfo(null)
    try {
      const c = await api.post<Comprovante>('/api/registros', {
        vinculoId: vinculoTrimmed,
        tipo,
        origem: 'TOTEM',
        offline: false,
        idempotencyKey: crypto.randomUUID(),
      })
      setInfo(`✅ ${c.tipo} confirmado — NSR ${c.nsr}`)
      setVinculo('')
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : `Falha ao registrar: ${String(e)}`
      setErro(msg)
    } finally {
      setEnviando(false)
    }
  }

  async function baterPorMatricula() {
    const m = vinculo.trim()
    if (!m) { setErro('Informe a matrícula.'); return }
    setEnviando(true); setErro(null); setInfo(null)
    try {
      const r = await api.post<{ nsr: number; rotuloTipo?: string; mensagem?: string }>(
        `/api/totem/bater?matricula=${encodeURIComponent(m)}`)
      setInfo(`✅ ${r.mensagem ?? `Registro NSR ${r.nsr}`}`)
      setVinculo('')
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : `Falha ao registrar: ${String(e)}`)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Totem de Ponto' }]} />
      <h1 className="page-title">Totem de Registro</h1>
      <p className="page-subtitle">Registro de ponto via quiosque</p>

      {tenantId && (
        <p style={{ fontSize: 13, color: 'var(--color-gray-60)', marginBottom: 16 }}>
          Tenant ativo: <code>{tenantId}</code>
        </p>
      )}

      <div className="br-card" style={{ maxWidth: 520, margin: '0 auto', textAlign: 'center' }}>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="totem-vinculo">
            Matrícula / ID do Vínculo
          </label>
          <input
            id="totem-vinculo"
            type="text"
            className="br-input"
            value={vinculo}
            onChange={(e) => { setVinculo(e.target.value); setErro(null); setInfo(null) }}
            placeholder="Ex: 0001234 ou UUID do vínculo"
            style={{ fontSize: 20, padding: 14, textAlign: 'center' }}
            disabled={enviando}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && vinculo.trim()) void registrar('ENTRADA')
            }}
            autoFocus
          />
        </div>

        <button
          disabled={enviando}
          onClick={() => void baterPorMatricula()}
          style={{
            width: '100%', fontSize: 20, padding: '20px 8px', cursor: 'pointer', marginTop: 8,
            background: 'var(--color-primary)', color: '#fff', border: 'none',
            borderRadius: 'var(--radius)', fontWeight: 800, opacity: enviando ? .5 : 1,
          }}
          aria-label="Registrar ponto por matrícula (tipo automático)"
        >
          ⏱ Registrar ponto (por matrícula)
        </button>
        <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '8px 0 0' }}>
          Digite a matrícula e toque acima — o tipo (entrada/intervalo/saída) é deduzido. Ou use os botões por tipo (exigem o ID do vínculo):
        </p>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 8 }}>
          {TIPOS.map((t) => (
            <button
              key={t.key}
              disabled={enviando}
              onClick={() => void registrar(t.key)}
              style={{
                fontSize: 16,
                padding: '18px 8px',
                cursor: 'pointer',
                background: t.color,
                color: '#fff',
                border: 'none',
                borderRadius: 'var(--radius)',
                fontWeight: 700,
                opacity: enviando ? .5 : 1,
              }}
              aria-label={`Registrar ${t.label}`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {info && (
          <div className="br-alert br-alert--success" role="alert" style={{ marginTop: 20 }}>
            {info}
          </div>
        )}
        {erro && (
          <div className="br-alert br-alert--danger" role="alert" style={{ marginTop: 20 }}>
            {erro}
          </div>
        )}
      </div>
    </>
  )
}
