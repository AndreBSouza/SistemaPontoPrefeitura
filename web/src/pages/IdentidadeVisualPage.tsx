import { FormEvent, useEffect, useState } from 'react'

import { api, ApiError } from '../api/client'
import { BrandingResponse, DefinirBrandingRequest } from '../api/types'

/** Identidade visual (white-label) do ente: nome do app, logo e cores. */
function norm(c: string) {
  return c.startsWith('#') ? c : `#${c}`
}

export default function IdentidadeVisualPage() {
  const [nomeApp, setNomeApp] = useState('Ponto Municipal')
  const [logoUrl, setLogoUrl] = useState('')
  const [corPrimaria, setCorPrimaria] = useState('#1351B4')
  const [corAcento, setCorAcento] = useState('#1F6E5C')
  const [carregando, setCarregando] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const [enviandoLogo, setEnviandoLogo] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [ok, setOk] = useState<string | null>(null)

  const [subdominio, setSubdominio] = useState('')
  const [cnpj, setCnpj] = useState('')

  async function salvarSubdominio() {
    setErro(null)
    setOk(null)
    try {
      await api.put<void>(`/api/branding/subdominio?valor=${encodeURIComponent(subdominio.trim().toLowerCase())}`)
      setOk('Subdomínio definido. O app/login pode se autoconfigurar por ele (apontamento DNS à parte).')
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : String(e))
    }
  }

  async function salvarCnpj() {
    setErro(null)
    setOk(null)
    try {
      await api.put<void>(`/api/branding/cnpj?valor=${encodeURIComponent(cnpj.replace(/\D/g, ''))}`)
      setOk('CNPJ do ente salvo. Ele passa a compor o cabeçalho do AFD (Portaria 671).')
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : String(e))
    }
  }

  async function enviarLogo(file: File) {
    setEnviandoLogo(true)
    setErro(null)
    setOk(null)
    try {
      const bytes = await file.arrayBuffer()
      const b = await api.postBinary<BrandingResponse>('/api/branding/logo', bytes, file.type)
      setLogoUrl(b.logoUrl ?? '')
      setOk('Logo enviado e armazenado. O app e o painel já exibem a nova imagem.')
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : String(e))
    } finally {
      setEnviandoLogo(false)
    }
  }

  useEffect(() => {
    api
      .get<BrandingResponse>('/api/branding')
      .then((b) => {
        setNomeApp(b.nomeApp)
        setLogoUrl(b.logoUrl ?? '')
        setCorPrimaria(norm(b.corPrimaria))
        setCorAcento(norm(b.corAcento))
        setCnpj(b.cnpj ?? '')
      })
      .catch((e) => setErro(e instanceof ApiError ? e.message : String(e)))
      .finally(() => setCarregando(false))
  }, [])

  async function salvar(e: FormEvent) {
    e.preventDefault()
    setSalvando(true)
    setErro(null)
    setOk(null)
    try {
      const body: DefinirBrandingRequest = {
        nomeApp: nomeApp || undefined,
        logoUrl: logoUrl || null,
        corPrimaria,
        corAcento,
      }
      const b = await api.put<BrandingResponse>('/api/branding', body)
      document.documentElement.style.setProperty('--color-primary', norm(b.corPrimaria))
      setOk('Identidade visual atualizada. O app e o painel passam a usar essas configurações.')
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : String(e))
    } finally {
      setSalvando(false)
    }
  }

  if (carregando) return <p>Carregando…</p>

  return (
    <>
      <h1 className="page-title">Identidade visual</h1>
      <p className="page-subtitle">
        Personalize o app e o painel da sua prefeitura: nome, logo e cores.
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0,1fr) 320px', gap: 24, alignItems: 'start' }}>
        <form className="br-card" onSubmit={salvar}>
          <div className="br-form-group">
            <label className="br-label" htmlFor="nome-app">Nome exibido no app</label>
            <input id="nome-app" className="br-input" maxLength={60} value={nomeApp}
              onChange={(e) => setNomeApp(e.target.value)} placeholder="Ex.: Ponto Goiânia" />
          </div>

          <div className="br-form-group">
            <label className="br-label" htmlFor="logo-url">URL do logo</label>
            <input id="logo-url" className="br-input" maxLength={500} value={logoUrl}
              onChange={(e) => setLogoUrl(e.target.value)} placeholder="https://.../logo.png" />
            <small style={{ color: 'var(--color-gray-60, #666)' }}>
              Cole o endereço de um logo já hospedado, ou envie o arquivo abaixo (fica guardado no sistema).
            </small>
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="logo-file">Enviar logo (PNG/JPG/SVG)</label>
            <input id="logo-file" type="file" className="br-input" accept="image/*" disabled={enviandoLogo}
              onChange={(e) => { const f = e.target.files?.[0]; if (f) void enviarLogo(f) }} />
            {enviandoLogo && <small style={{ color: 'var(--color-gray-60, #666)' }}>Enviando…</small>}
          </div>

          <div className="form-row" style={{ display: 'flex', gap: 24 }}>
            <div className="br-form-group">
              <label className="br-label" htmlFor="cor-primaria">Cor primária</label>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <input id="cor-primaria" type="color" value={corPrimaria}
                  onChange={(e) => setCorPrimaria(e.target.value)} style={{ width: 48, height: 40 }} />
                <input className="br-input" value={corPrimaria} maxLength={7}
                  onChange={(e) => setCorPrimaria(norm(e.target.value))} style={{ width: 110 }} />
              </div>
            </div>
            <div className="br-form-group">
              <label className="br-label" htmlFor="cor-acento">Cor de acento</label>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <input id="cor-acento" type="color" value={corAcento}
                  onChange={(e) => setCorAcento(e.target.value)} style={{ width: 48, height: 40 }} />
                <input className="br-input" value={corAcento} maxLength={7}
                  onChange={(e) => setCorAcento(norm(e.target.value))} style={{ width: 110 }} />
              </div>
            </div>
          </div>

          {/* Identificação do ente — dentro do card para o feedback (erro/ok) aparecer junto dos
              botões e para o grid manter só 2 colunas (form | pré-visualização). */}
          <div className="br-form-group" style={{ marginTop: 24, borderTop: '1px solid var(--color-gray-8)', paddingTop: 16 }}>
            <label className="br-label" htmlFor="subdominio">Subdomínio do ente</label>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <input id="subdominio" className="br-input" maxLength={60} value={subdominio}
                onChange={(e) => setSubdominio(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ''))}
                placeholder="ex.: cidade-uf" style={{ maxWidth: 240 }} />
              <button type="button" className="br-button br-button--secondary" onClick={() => void salvarSubdominio()}
                disabled={!subdominio.trim()}>Salvar subdomínio</button>
            </div>
            <small style={{ color: 'var(--color-gray-60, #666)' }}>
              O app/login se autoconfigura por ele (ex.: cidade-uf.ponto.gov.br). O apontamento DNS é feito à parte.
            </small>

            <label className="br-label" htmlFor="cnpj" style={{ marginTop: 16 }}>CNPJ do ente</label>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <input id="cnpj" className="br-input" inputMode="numeric" maxLength={18} value={cnpj}
                onChange={(e) => setCnpj(e.target.value.replace(/[^\d./-]/g, ''))}
                placeholder="00.000.000/0001-00" style={{ maxWidth: 240 }} />
              <button type="button" className="br-button br-button--secondary" onClick={() => void salvarCnpj()}
                disabled={cnpj.replace(/\D/g, '').length !== 14}>Salvar CNPJ</button>
            </div>
            <small style={{ color: 'var(--color-gray-60, #666)' }}>
              Obrigatório para emitir o AFD (arquivo-fonte de dados, Portaria MTP 671/2021).
            </small>
          </div>

          {erro && <div className="br-alert br-alert--danger" role="alert" style={{ marginTop: 12 }}>{erro}</div>}
          {ok && <div className="br-alert br-alert--success" role="alert" style={{ marginTop: 12 }}>{ok}</div>}

          <button className="br-button br-button--primary" type="submit" disabled={salvando} style={{ marginTop: 12 }}>
            {salvando ? 'Salvando…' : 'Salvar identidade'}
          </button>
        </form>

        {/* Pré-visualização */}
        <div className="br-card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ background: corPrimaria, color: '#fff', padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 8 }}>
            {logoUrl ? <img src={logoUrl} alt="" style={{ height: 24 }} /> : <span aria-hidden="true">🏛️</span>}
            <strong>{nomeApp || 'Ponto Municipal'}</strong>
          </div>
          <div style={{ padding: 16 }}>
            <p style={{ marginTop: 0, color: 'var(--color-gray-60)' }}>Pré-visualização</p>
            <button style={{ background: corPrimaria, color: '#fff', border: 'none', borderRadius: 6, padding: '10px 16px', fontWeight: 700 }}>
              Registrar ponto
            </button>
            <div style={{ marginTop: 12, display: 'inline-block', background: corAcento, color: '#fff', borderRadius: 999, padding: '2px 10px', fontSize: 13 }}>
              Registrado
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
