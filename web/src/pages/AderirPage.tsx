import { useState, FormEvent } from 'react'

/**
 * Página PÚBLICA de adesão self-service (12.3.13): um município solicita a adesão. A solicitação
 * fica pendente e só vira um ente de fato após aprovação do operador da plataforma.
 */
export default function AderirPage() {
  const [nome, setNome] = useState('')
  const [slug, setSlug] = useState('')
  const [tipoPoder, setTipoPoder] = useState('EXECUTIVO')
  const [responsavelNome, setResponsavelNome] = useState('')
  const [responsavelEmail, setResponsavelEmail] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [ok, setOk] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setEnviando(true)
    setErro(null)
    try {
      const resp = await fetch('/api/publico/onboarding', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nome, slug, tipoPoder, responsavelNome, responsavelEmail }),
      })
      if (resp.status === 429) {
        throw new Error('Muitas solicitações deste endereço. Tente novamente mais tarde.')
      }
      if (!resp.ok) {
        const txt = await resp.text()
        throw new Error(txt || 'Não foi possível enviar a solicitação.')
      }
      setOk(true)
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro inesperado.')
    } finally {
      setEnviando(false)
    }
  }

  if (ok) {
    return (
      <div style={{ maxWidth: 560, margin: '48px auto', padding: 24 }}>
        <div className="br-alert br-alert--success" role="alert">
          <strong>Solicitação enviada!</strong> Recebemos o pedido de adesão de <strong>{nome}</strong>.
          Nossa equipe vai analisar e entrar em contato pelo e-mail informado.
        </div>
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 560, margin: '48px auto', padding: 24 }}>
      <h1 className="page-title">Solicitar adesão ao Ponto Municipal</h1>
      <p className="page-subtitle">
        Preencha os dados do ente. A adesão é confirmada após análise da nossa equipe.
      </p>
      {erro && <div className="br-alert br-alert--danger" role="alert" style={{ marginBottom: 12 }}>{erro}</div>}
      <form onSubmit={enviar} noValidate>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="o-nome">Nome do ente</label>
          <input id="o-nome" className="br-input" value={nome} onChange={(e) => setNome(e.target.value)}
                 required placeholder="Prefeitura Municipal de ..." />
        </div>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="o-slug">Identificador (slug)</label>
          <input id="o-slug" className="br-input" value={slug}
                 onChange={(e) => setSlug(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ''))}
                 required placeholder="ex.: cidade-uf" pattern="[a-z0-9-]{2,60}" />
          <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '4px 0 0' }}>
            Apenas letras minúsculas, números e hífen.
          </p>
        </div>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="o-poder">Poder</label>
          <select id="o-poder" className="br-select" value={tipoPoder} onChange={(e) => setTipoPoder(e.target.value)}>
            <option value="EXECUTIVO">Executivo</option>
            <option value="LEGISLATIVO">Legislativo</option>
          </select>
        </div>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="o-resp">Nome do responsável</label>
          <input id="o-resp" className="br-input" value={responsavelNome}
                 onChange={(e) => setResponsavelNome(e.target.value)} required />
        </div>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="o-email">E-mail do responsável</label>
          <input id="o-email" type="email" className="br-input" value={responsavelEmail}
                 onChange={(e) => setResponsavelEmail(e.target.value)} required />
        </div>
        <button className="br-button br-button--primary" type="submit" disabled={enviando}>
          {enviando ? 'Enviando…' : 'Enviar solicitação'}
        </button>
      </form>
    </div>
  )
}
