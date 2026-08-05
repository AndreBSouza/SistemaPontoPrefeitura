import { useEffect, useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type {
  CorrecaoResponse,
  CorrecaoLoteRequest,
  ServidorResponse,
  TipoMarcacao,
} from '../api/types'

const TIPO_LABEL: Record<TipoMarcacao, string> = {
  ENTRADA: 'Entrada',
  SAIDA: 'Saída',
  INTERVALO_INICIO: 'Início intervalo',
  INTERVALO_FIM: 'Fim intervalo',
}

interface ItemLocal {
  dataHoraLocal: string
  tipo: TipoMarcacao
}

export default function CorrecoesPage() {
  const [pendentes, setPendentes] = useState<CorrecaoResponse[]>([])
  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')
  const [processando, setProcessando] = useState<string | null>(null)

  // Correção direta do RH (lote).
  const [vinculoId, setVinculoId] = useState('')
  const [motivo, setMotivo] = useState('')
  const [novaDataHora, setNovaDataHora] = useState('')
  const [novoTipo, setNovoTipo] = useState<TipoMarcacao>('ENTRADA')
  const [itens, setItens] = useState<ItemLocal[]>([])
  const [aplicando, setAplicando] = useState(false)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [p, s] = await Promise.all([
        api.get<CorrecaoResponse[]>('/api/correcoes/pendentes'),
        api.get<ServidorResponse[]>('/api/servidores'),
      ])
      setPendentes(p)
      setServidores(s)
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [])

  function flash(msg: string) {
    setSucesso(msg)
    setTimeout(() => setSucesso(''), 4000)
  }

  const vinculos = servidores.flatMap((s) => s.vinculos.map((v) => ({ v, s })))

  async function decidir(id: string, acao: 'aprovar' | 'rejeitar') {
    setProcessando(id)
    setError(null)
    try {
      await api.post<CorrecaoResponse>(`/api/correcoes/${id}/${acao}`, {
        motivoDecisao: acao === 'aprovar' ? 'Deferido' : 'Indeferido',
      })
      flash(`Correção ${acao === 'aprovar' ? 'aprovada (marcação criada)' : 'recusada'}.`)
      void load()
    } catch (e) {
      setError(e)
    } finally {
      setProcessando(null)
    }
  }

  function adicionarItem() {
    if (!novaDataHora) return
    setItens((prev) => [...prev, { dataHoraLocal: novaDataHora, tipo: novoTipo }])
    setNovaDataHora('')
  }

  function removerItem(idx: number) {
    setItens((prev) => prev.filter((_, i) => i !== idx))
  }

  async function aplicarLote() {
    if (!vinculoId || itens.length === 0) return
    setAplicando(true)
    setError(null)
    try {
      const body: CorrecaoLoteRequest = {
        vinculoId,
        motivo: motivo.trim() || undefined,
        itens: itens.map((i) => ({ dataHora: new Date(i.dataHoraLocal).toISOString(), tipo: i.tipo })),
      }
      const criadas = await api.post<CorrecaoResponse[]>('/api/correcoes/lote', body)
      flash(`${criadas.length} marcação(ões) criada(s) por correção.`)
      setItens([])
      setMotivo('')
      void load()
    } catch (e) {
      setError(e)
    } finally {
      setAplicando(false)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Correções de marcação' }]} />
      <h1 className="page-title">Correções de marcação</h1>
      <p className="page-subtitle">
        Aprove "esqueci de bater" dos servidores ou faça correções diretas. A marcação aprovada é uma nova batida auditada (registros são imutáveis).
      </p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <h3 className="br-card__title" style={{ marginBottom: 8 }}>Solicitações pendentes</h3>
      {loading ? <Loading /> : pendentes.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">✅</div>
          <p className="empty-state__title">Nenhuma correção pendente</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Correções pendentes</caption>
            <thead>
              <tr>
                <th scope="col">Vínculo</th>
                <th scope="col">Marcação</th>
                <th scope="col">Tipo</th>
                <th scope="col">Motivo</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              {pendentes.map((c) => (
                <tr key={c.id}>
                  <td style={{ fontSize: 12, fontFamily: 'monospace' }}>{c.vinculoId}</td>
                  <td>{new Date(c.dataHora).toLocaleString('pt-BR')}</td>
                  <td>{TIPO_LABEL[c.tipo]}</td>
                  <td style={{ maxWidth: 220 }}>{c.motivo}</td>
                  <td>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button className="br-button br-button--success br-button--sm"
                        onClick={() => void decidir(c.id, 'aprovar')} disabled={processando === c.id}>
                        Aprovar
                      </button>
                      <button className="br-button br-button--danger br-button--sm"
                        onClick={() => void decidir(c.id, 'rejeitar')} disabled={processando === c.id}>
                        Recusar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Correção direta do RH (lote) */}
      <div className="br-card" style={{ marginTop: 32 }}>
        <h3 className="br-card__title" style={{ marginBottom: 12 }}>Correção direta (RH) — em lote</h3>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="cor-vinculo">Vínculo</label>
          <select id="cor-vinculo" className="br-select" value={vinculoId} onChange={(e) => setVinculoId(e.target.value)}>
            <option value="">— Selecione —</option>
            {vinculos.map(({ v, s }) => (
              <option key={v.id} value={v.id}>{s.nome} — {v.matricula}</option>
            ))}
          </select>
        </div>

        <div className="form-row" style={{ gap: 12, alignItems: 'flex-end' }}>
          <div className="br-form-group" style={{ marginBottom: 0 }}>
            <label className="br-label" htmlFor="cor-dh">Data e hora</label>
            <input id="cor-dh" type="datetime-local" className="br-input" value={novaDataHora} onChange={(e) => setNovaDataHora(e.target.value)} />
          </div>
          <div className="br-form-group" style={{ marginBottom: 0 }}>
            <label className="br-label" htmlFor="cor-tipo">Tipo</label>
            <select id="cor-tipo" className="br-select" value={novoTipo} onChange={(e) => setNovoTipo(e.target.value as TipoMarcacao)}>
              <option value="ENTRADA">Entrada</option>
              <option value="SAIDA">Saída</option>
              <option value="INTERVALO_INICIO">Início intervalo</option>
              <option value="INTERVALO_FIM">Fim intervalo</option>
            </select>
          </div>
          <button type="button" className="br-button br-button--secondary br-button--sm" onClick={adicionarItem} disabled={!novaDataHora}>
            + Adicionar marcação
          </button>
        </div>

        {itens.length > 0 && (
          <ul style={{ margin: '12px 0', paddingLeft: 18 }}>
            {itens.map((i, idx) => (
              <li key={idx} style={{ marginBottom: 4 }}>
                {new Date(i.dataHoraLocal).toLocaleString('pt-BR')} — {TIPO_LABEL[i.tipo]}{' '}
                <button type="button" className="br-button br-button--tertiary br-button--sm" onClick={() => removerItem(idx)}>remover</button>
              </li>
            ))}
          </ul>
        )}

        <div className="br-form-group">
          <label className="br-label" htmlFor="cor-motivo">Motivo</label>
          <input id="cor-motivo" className="br-input" value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={500} placeholder="ex: relógio em manutenção" />
        </div>
        <button className="br-button br-button--primary" onClick={() => void aplicarLote()} disabled={aplicando || !vinculoId || itens.length === 0}>
          {aplicando ? 'Aplicando…' : `Aplicar correção (${itens.length})`}
        </button>
      </div>
    </>
  )
}
