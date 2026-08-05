import { useEffect, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type {
  ContratoResponse,
  ContratoRequest,
  ExecucaoContratoResponse,
  ModalidadeContratacao,
} from '../api/types'

const MODALIDADES: Array<{ value: ModalidadeContratacao; label: string }> = [
  { value: 'DISPENSA', label: 'Dispensa de licitação' },
  { value: 'PREGAO', label: 'Pregão eletrônico' },
  { value: 'INEXIGIBILIDADE', label: 'Inexigibilidade' },
  { value: 'CONCORRENCIA', label: 'Concorrência' },
  { value: 'ADESAO_ATA', label: 'Adesão a ata de registro de preços' },
  { value: 'OUTRA', label: 'Outra' },
]

function brl(v: number | null): string {
  if (v == null) return '—'
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v)
}

function mesAtual(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

export default function ContratoPage() {
  const [contratos, setContratos] = useState<ContratoResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [sucesso, setSucesso] = useState('')

  // formulário
  const [modalidade, setModalidade] = useState<ModalidadeContratacao>('PREGAO')
  const [numeroProcesso, setNumeroProcesso] = useState('')
  const [empenho, setEmpenho] = useState('')
  const [vigenciaInicio, setVigenciaInicio] = useState('')
  const [vigenciaFim, setVigenciaFim] = useState('')
  const [valorGlobal, setValorGlobal] = useState('')
  const [valorMensal, setValorMensal] = useState('')
  const [observacao, setObservacao] = useState('')
  const [saving, setSaving] = useState(false)

  // relatório de execução
  const [competencia, setCompetencia] = useState(mesAtual())
  const [execucao, setExecucao] = useState<ExecucaoContratoResponse | null>(null)
  const [buscandoExec, setBuscandoExec] = useState(false)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setContratos(await api.get<ContratoResponse[]>('/api/contratos'))
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }
  useEffect(() => { void load() }, [])

  function flash(msg: string) {
    setSucesso(msg)
    setTimeout(() => setSucesso(''), 5000)
  }

  async function salvar(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body: ContratoRequest = {
        modalidade,
        numeroProcesso: numeroProcesso.trim() || undefined,
        empenho: empenho.trim() || undefined,
        vigenciaInicio,
        vigenciaFim,
        valorGlobal: valorGlobal ? Number(valorGlobal) : undefined,
        valorMensal: valorMensal ? Number(valorMensal) : undefined,
        observacao: observacao.trim() || undefined,
      }
      await api.post<ContratoResponse>('/api/contratos', body)
      setNumeroProcesso(''); setEmpenho(''); setVigenciaInicio(''); setVigenciaFim('')
      setValorGlobal(''); setValorMensal(''); setObservacao('')
      void load()
      flash('Contrato registrado.')
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function remover(id: string) {
    if (!window.confirm('Remover este contrato?')) return
    setError(null)
    try {
      await api.delete<void>(`/api/contratos/${id}`)
      void load()
      flash('Contrato removido.')
    } catch (err) {
      setError(err)
    }
  }

  async function gerarExecucao() {
    setBuscandoExec(true)
    setError(null)
    try {
      setExecucao(await api.get<ExecucaoContratoResponse>(`/api/contratos/execucao?competencia=${competencia}`))
    } catch (err) {
      setError(err)
    } finally {
      setBuscandoExec(false)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Contrato' }]} />
      <h1 className="page-title">Contrato</h1>
      <p className="page-subtitle">
        Contrato de fornecimento do sistema ao ente — valor fixo (dispensa/licitação), não cobrança por demanda.
      </p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error != null && <ErrorAlert error={error} />}

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0,1fr) 340px', gap: 24, alignItems: 'start' }}>
        {/* Novo contrato */}
        <form className="br-card" onSubmit={salvar} noValidate>
          <h2 className="br-card__title" style={{ marginBottom: 12 }}>Novo contrato</h2>
          <div className="form-row">
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="ct-modalidade">Modalidade</label>
              <select id="ct-modalidade" className="br-select" value={modalidade}
                onChange={(e) => setModalidade(e.target.value as ModalidadeContratacao)}>
                {MODALIDADES.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
              </select>
            </div>
            <div className="br-form-group">
              <label className="br-label" htmlFor="ct-processo">Nº processo / contrato</label>
              <input id="ct-processo" className="br-input" maxLength={60} value={numeroProcesso}
                onChange={(e) => setNumeroProcesso(e.target.value)} placeholder="ex: PE 01/2026" />
            </div>
          </div>
          <div className="form-row">
            <div className="br-form-group">
              <label className="br-label" htmlFor="ct-empenho">Nota de empenho</label>
              <input id="ct-empenho" className="br-input" maxLength={60} value={empenho}
                onChange={(e) => setEmpenho(e.target.value)} placeholder="ex: 2026NE000123" />
            </div>
          </div>
          <div className="form-row">
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="ct-ini">Vigência início</label>
              <input id="ct-ini" type="date" className="br-input" value={vigenciaInicio}
                onChange={(e) => setVigenciaInicio(e.target.value)} required />
            </div>
            <div className="br-form-group">
              <label className="br-label br-label--required" htmlFor="ct-fim">Vigência fim</label>
              <input id="ct-fim" type="date" className="br-input" value={vigenciaFim}
                onChange={(e) => setVigenciaFim(e.target.value)} required />
            </div>
          </div>
          <div className="form-row">
            <div className="br-form-group">
              <label className="br-label" htmlFor="ct-global">Valor global (R$)</label>
              <input id="ct-global" type="number" min="0" step="0.01" className="br-input" value={valorGlobal}
                onChange={(e) => setValorGlobal(e.target.value)} placeholder="ex: 30000.00" />
            </div>
            <div className="br-form-group">
              <label className="br-label" htmlFor="ct-mensal">Parcela mensal (R$)</label>
              <input id="ct-mensal" type="number" min="0" step="0.01" className="br-input" value={valorMensal}
                onChange={(e) => setValorMensal(e.target.value)} placeholder="ex: 2500.00" />
            </div>
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="ct-obs">Observação</label>
            <input id="ct-obs" className="br-input" maxLength={500} value={observacao}
              onChange={(e) => setObservacao(e.target.value)} />
          </div>
          <button className="br-button br-button--primary" type="submit" disabled={saving}>
            {saving ? 'Salvando…' : 'Registrar contrato'}
          </button>
        </form>

        {/* Relatório de execução */}
        <div className="br-card">
          <h2 className="br-card__title" style={{ marginBottom: 8 }}>Relatório de execução</h2>
          <p style={{ fontSize: 13, color: 'var(--color-gray-60)', marginTop: 0 }}>
            Para anexar ao processo de pagamento/liquidação do mês.
          </p>
          <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end', marginBottom: 12 }}>
            <div className="br-form-group" style={{ marginBottom: 0, flex: 1 }}>
              <label className="br-label" htmlFor="ct-comp">Competência</label>
              <input id="ct-comp" type="month" className="br-input" value={competencia}
                onChange={(e) => setCompetencia(e.target.value)} />
            </div>
            <button className="br-button br-button--secondary" onClick={() => void gerarExecucao()} disabled={buscandoExec}>
              {buscandoExec ? '…' : 'Gerar'}
            </button>
          </div>
          {execucao && (
            <dl style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: '6px 12px', fontSize: 14 }}>
              <dt style={{ color: 'var(--color-gray-60)' }}>Contrato vigente</dt>
              <dd style={{ margin: 0, fontWeight: 600 }}>{execucao.contratoVigente ? 'Sim' : 'Não'}</dd>
              <dt style={{ color: 'var(--color-gray-60)' }}>Parcela do mês</dt>
              <dd style={{ margin: 0, fontWeight: 700, color: 'var(--color-primary)' }}>{brl(execucao.valorMensal)}</dd>
              <dt style={{ color: 'var(--color-gray-60)' }}>Servidores atendidos</dt>
              <dd style={{ margin: 0, fontWeight: 600 }}>{execucao.servidoresAtivos}</dd>
              <dt style={{ color: 'var(--color-gray-60)' }}>Dispositivos ativos</dt>
              <dd style={{ margin: 0, fontWeight: 600 }}>{execucao.dispositivosAtivos}</dd>
              <dt style={{ color: 'var(--color-gray-60)' }}>Registros no período</dt>
              <dd style={{ margin: 0, fontWeight: 600 }}>{execucao.registrosNoPeriodo}</dd>
              {execucao.numeroProcesso && (<>
                <dt style={{ color: 'var(--color-gray-60)' }}>Processo</dt>
                <dd style={{ margin: 0 }}>{execucao.numeroProcesso}</dd>
              </>)}
              {execucao.empenho && (<>
                <dt style={{ color: 'var(--color-gray-60)' }}>Empenho</dt>
                <dd style={{ margin: 0 }}>{execucao.empenho}</dd>
              </>)}
            </dl>
          )}
        </div>
      </div>

      <h2 className="br-card__title" style={{ margin: '24px 0 8px' }}>Contratos</h2>
      {loading ? (
        <Loading />
      ) : contratos.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">📄</div>
          <p className="empty-state__title">Nenhum contrato registrado</p>
          <p>Use o formulário acima para registrar o contrato vigente.</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Contratos do ente</caption>
            <thead>
              <tr>
                <th scope="col">Modalidade</th>
                <th scope="col">Processo</th>
                <th scope="col">Vigência</th>
                <th scope="col">Valor global</th>
                <th scope="col">Parcela mensal</th>
                <th scope="col">Situação</th>
                <th scope="col">Ação</th>
              </tr>
            </thead>
            <tbody>
              {contratos.map((c) => (
                <tr key={c.id}>
                  <td>{c.modalidadeRotulo}</td>
                  <td style={{ fontSize: 13 }}>{c.numeroProcesso ?? '—'}</td>
                  <td style={{ fontSize: 13 }}>{c.vigenciaInicio} a {c.vigenciaFim}</td>
                  <td>{brl(c.valorGlobal)}</td>
                  <td>{brl(c.valorMensal)}</td>
                  <td>
                    {c.vigente
                      ? <span className="br-badge br-badge--success">Vigente</span>
                      : <span className="br-badge br-badge--neutral">Encerrado/futuro</span>}
                  </td>
                  <td>
                    <button className="br-button br-button--sm br-button--danger"
                      onClick={() => void remover(c.id)}
                      aria-label={`Remover contrato ${c.numeroProcesso ?? ''}`}>
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
