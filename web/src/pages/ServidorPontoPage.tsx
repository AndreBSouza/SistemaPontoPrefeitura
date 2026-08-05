import { useState } from 'react'

import { api, ApiError } from '../api/client'

/**
 * Acesso do SERVIDOR pela web — bater ponto no computador do trabalho ou no
 * navegador do celular. Reaproveita a autenticação por dispositivo: o servidor
 * ativa o navegador com o código do RH (token guardado neste navegador) e usa o
 * botão único, igual ao app (POST /api/me/bater).
 */
const TOKEN_KEY = 'pm_device_token'
const NOME_KEY = 'pm_servidor_nome'

interface AtivacaoResult {
  deviceToken: string
  nome: string
}
interface Batida {
  tipo: string
  nsr: number
  mensagem?: string
}

export default function ServidorPontoPage() {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))
  const [nome, setNome] = useState<string | null>(() => localStorage.getItem(NOME_KEY))
  const [codigo, setCodigo] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [ocupado, setOcupado] = useState(false)

  async function ativar() {
    const c = codigo.trim()
    if (!c) {
      setErro('Digite o código de ativação fornecido pelo RH.')
      return
    }
    setOcupado(true)
    setErro(null)
    try {
      const r = await api.post<AtivacaoResult>('/api/ativacao/ativar', {
        codigo: c,
        nomeDispositivo: 'Navegador Web',
      })
      localStorage.setItem(TOKEN_KEY, r.deviceToken)
      localStorage.setItem(NOME_KEY, r.nome ?? 'Servidor')
      setToken(r.deviceToken)
      setNome(r.nome ?? 'Servidor')
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : `Falha ao ativar: ${String(e)}`)
    } finally {
      setOcupado(false)
    }
  }

  async function localizacao(): Promise<{ latitude: number | null; longitude: number | null }> {
    return new Promise((resolve) => {
      if (!navigator.geolocation) return resolve({ latitude: null, longitude: null })
      navigator.geolocation.getCurrentPosition(
        (p) => resolve({ latitude: p.coords.latitude, longitude: p.coords.longitude }),
        () => resolve({ latitude: null, longitude: null }),
        { timeout: 5000 },
      )
    })
  }

  async function bater() {
    setOcupado(true)
    setErro(null)
    setInfo(null)
    try {
      const pos = await localizacao()
      const b = await api.post<Batida>('/api/me/bater', {
        origem: 'WEB',
        idempotencyKey: crypto.randomUUID(),
        latitude: pos.latitude,
        longitude: pos.longitude,
        offline: false,
        dataHoraDispositivo: new Date().toISOString(),
      })
      setInfo(b.mensagem ?? `${b.tipo} registrada — NSR ${b.nsr}`)
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        sair()
        setErro('Sessão expirada ou dispositivo revogado. Ative novamente com um novo código.')
      } else {
        setErro(e instanceof ApiError ? e.message : `Falha ao registrar: ${String(e)}`)
      }
    } finally {
      setOcupado(false)
    }
  }

  function sair() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(NOME_KEY)
    setToken(null)
    setNome(null)
    setInfo(null)
  }

  return (
    <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center', background: 'var(--color-gray-2, #f8f8f8)' }}>
      <div className="br-card" style={{ maxWidth: 460, width: '100%', textAlign: 'center', padding: 28 }}>
        <h1 className="page-title" style={{ color: 'var(--color-primary, #1351B4)' }}>Ponto Municipal</h1>

        {!token ? (
          <>
            <p className="page-subtitle">Ative este navegador com o código do RH</p>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="codigo">Código de ativação</label>
              <input
                id="codigo"
                className="br-input"
                value={codigo}
                onChange={(e) => { setCodigo(e.target.value.toUpperCase()); setErro(null) }}
                placeholder="AAAA-AAAA"
                style={{ fontSize: 22, padding: 14, textAlign: 'center', letterSpacing: 2 }}
                disabled={ocupado}
                onKeyDown={(e) => { if (e.key === 'Enter') void ativar() }}
                autoFocus
              />
            </div>
            <button className="br-button br-button--primary" onClick={() => void ativar()} disabled={ocupado}
              style={{ width: '100%', fontSize: 18, padding: 14 }}>
              {ocupado ? 'Ativando…' : 'Ativar'}
            </button>
          </>
        ) : (
          <>
            <p className="page-subtitle">Olá, {nome}</p>
            <button onClick={() => void bater()} disabled={ocupado} aria-label="Registrar ponto"
              style={{
                width: 200, height: 200, borderRadius: '50%', margin: '12px auto',
                background: 'var(--color-primary, #1351B4)', color: '#fff', border: 'none',
                fontSize: 20, fontWeight: 700, cursor: 'pointer', display: 'block',
                boxShadow: '0 6px 18px rgba(0,0,0,0.2)', whiteSpace: 'pre-line', opacity: ocupado ? 0.6 : 1,
              }}>
              {ocupado ? 'Registrando…' : 'REGISTRAR\nPONTO'}
            </button>
            <button className="br-button br-button--secondary" onClick={sair} disabled={ocupado}
              style={{ marginTop: 8 }}>
              Sair deste navegador
            </button>
          </>
        )}

        {info && <div className="br-alert br-alert--success" role="alert" style={{ marginTop: 20 }}>{info}</div>}
        {erro && <div className="br-alert br-alert--danger" role="alert" style={{ marginTop: 20 }}>{erro}</div>}
      </div>
    </div>
  )
}
