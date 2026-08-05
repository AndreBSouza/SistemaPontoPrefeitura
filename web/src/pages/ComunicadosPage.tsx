import { useEffect, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type { ComunicadoResponse, LotacaoResponse, PublicarComunicadoRequest } from '../api/types'

export default function ComunicadosPage() {
  const [comunicados, setComunicados] = useState<ComunicadoResponse[]>([])
  const [lotacoes, setLotacoes] = useState<LotacaoResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')

  const [titulo, setTitulo] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [lotacaoId, setLotacaoId] = useState('')
  const [saving, setSaving] = useState(false)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [c, l] = await Promise.all([
        api.get<ComunicadoResponse[]>('/api/comunicados'),
        api.get<LotacaoResponse[]>('/api/lotacoes'),
      ])
      setComunicados(c)
      setLotacoes(l)
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [])

  function orgaoNome(id: string | null): string {
    if (!id) return 'Todos os servidores'
    return lotacoes.find((l) => l.id === id)?.nome ?? id
  }

  async function publicar(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body: PublicarComunicadoRequest = {
        titulo: titulo.trim(),
        mensagem: mensagem.trim(),
        lotacaoId: lotacaoId || null,
      }
      await api.post<ComunicadoResponse>('/api/comunicados', body)
      setSucesso('Comunicado publicado.')
      setTimeout(() => setSucesso(''), 4000)
      setTitulo('')
      setMensagem('')
      setLotacaoId('')
      void load()
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Comunicados' }]} />
      <h1 className="page-title">Comunicados oficiais</h1>
      <p className="page-subtitle">Canal direto da prefeitura com os servidores (aparece no app)</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <div className="br-card" style={{ marginBottom: 24 }}>
        <h3 className="br-card__title" style={{ marginBottom: 12 }}>Novo comunicado</h3>
        <form onSubmit={publicar} noValidate>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="com-titulo">Título</label>
            <input
              id="com-titulo"
              className="br-input"
              value={titulo}
              onChange={(e) => setTitulo(e.target.value)}
              maxLength={200}
              required
            />
          </div>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="com-msg">Mensagem</label>
            <textarea
              id="com-msg"
              className="br-input"
              rows={4}
              value={mensagem}
              onChange={(e) => setMensagem(e.target.value)}
              maxLength={4000}
              required
            />
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="com-orgao">Destinatário</label>
            <select
              id="com-orgao"
              className="br-select"
              value={lotacaoId}
              onChange={(e) => setLotacaoId(e.target.value)}
            >
              <option value="">Todos os servidores (geral)</option>
              {lotacoes.map((l) => (
                <option key={l.id} value={l.id}>{l.nome}</option>
              ))}
            </select>
          </div>
          <button
            className="br-button br-button--primary"
            type="submit"
            disabled={saving || !titulo.trim() || !mensagem.trim()}
          >
            {saving ? 'Publicando…' : 'Publicar comunicado'}
          </button>
        </form>
      </div>

      <h3 className="br-card__title" style={{ marginBottom: 8 }}>Publicados</h3>
      {loading ? <Loading /> : comunicados.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">📣</div>
          <p className="empty-state__title">Nenhum comunicado publicado</p>
          <p>Use o formulário acima para enviar o primeiro comunicado.</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Comunicados publicados</caption>
            <thead>
              <tr>
                <th scope="col">Título</th>
                <th scope="col">Mensagem</th>
                <th scope="col">Destinatário</th>
                <th scope="col">Publicado em</th>
              </tr>
            </thead>
            <tbody>
              {comunicados.map((c) => (
                <tr key={c.id}>
                  <td>{c.titulo}</td>
                  <td style={{ maxWidth: 320 }}>{c.mensagem}</td>
                  <td>
                    {c.geral ? (
                      <span className="br-badge br-badge--info">Geral</span>
                    ) : (
                      <span className="br-badge br-badge--neutral">{orgaoNome(c.lotacaoId)}</span>
                    )}
                  </td>
                  <td>{new Date(c.publicadoEm).toLocaleString('pt-BR')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
