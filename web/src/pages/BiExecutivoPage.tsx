import { useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type { BiExecutivoResponse, SimulacaoResponse, RoiResponse } from '../api/types'

function hhmm(min: number) {
  const h = Math.floor(min / 60)
  const m = min % 60
  return `${h}h ${String(m).padStart(2, '0')}min`
}

const RISCO_BADGE: Record<string, string> = {
  OK: 'br-badge--success',
  ALERTA: 'br-badge--warning',
  PRUDENCIAL: 'br-badge--warning',
  ESTOURO: 'br-badge--danger',
}

export default function BiExecutivoPage() {
  const [competencia, setCompetencia] = useState(() => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  })
  const [bi, setBi] = useState<BiExecutivoResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<unknown>(null)

  // Simulador
  const [horasExtras, setHorasExtras] = useState('1000')
  const [custoHora, setCustoHora] = useState('30')
  const [gastoPessoal, setGastoPessoal] = useState('50000000')
  const [rcl, setRcl] = useState('100000000')
  const [sim, setSim] = useState<SimulacaoResponse | null>(null)

  // ROI / economia
  const [roiCustoHora, setRoiCustoHora] = useState('30')
  const [roiRedHe, setRoiRedHe] = useState('40')
  const [roiCustoFalta, setRoiCustoFalta] = useState('150')
  const [roiRedAbs, setRoiRedAbs] = useState('20')
  const [roi, setRoi] = useState<RoiResponse | null>(null)

  async function carregar() {
    setLoading(true)
    setError(null)
    try {
      const r = await api.get<BiExecutivoResponse>(`/api/relatorios/bi?competencia=${competencia}`)
      r.orgaos.sort((a, b) => b.horaExtraMinutos - a.horaExtraMinutos)
      setBi(r)
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  async function simular() {
    setError(null)
    try {
      const qs = `horasExtras=${horasExtras}&custoHora=${custoHora}&gastoPessoalAtual=${gastoPessoal}&rcl=${rcl}`
      setSim(await api.get<SimulacaoResponse>(`/api/relatorios/simulador?${qs}`))
    } catch (e) {
      setError(e)
    }
  }

  async function calcularRoi() {
    setError(null)
    try {
      const qs = `competencia=${competencia}&custoHora=${roiCustoHora}&reducaoHoraExtraPct=${roiRedHe}&custoDiaFalta=${roiCustoFalta}&reducaoAbsenteismoPct=${roiRedAbs}`
      setRoi(await api.get<RoiResponse>(`/api/relatorios/roi?${qs}`))
    } catch (e) {
      setError(e)
    }
  }

  const reais = (v: number) => v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'BI executivo' }]} />
      <h1 className="page-title">BI executivo</h1>
      <p className="page-subtitle">Presença, absenteísmo e custo de hora extra por secretaria — e simulador orçamentário (LRF)</p>

      {error && <ErrorAlert error={error} />}

      <div className="br-card" style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
          <div className="br-form-group" style={{ marginBottom: 0 }}>
            <label className="br-label" htmlFor="bi-comp">Competência</label>
            <input id="bi-comp" type="month" className="br-input" value={competencia} onChange={(e) => setCompetencia(e.target.value)} />
          </div>
          <button className="br-button br-button--primary" onClick={() => void carregar()} disabled={loading}>
            {loading ? 'Carregando…' : 'Gerar BI'}
          </button>
        </div>
      </div>

      {loading && <Loading />}

      {bi && (
        <>
          <div className="stat-grid">
            <div className="stat-card"><p className="stat-card__label">Vínculos</p><p className="stat-card__value">{bi.totalVinculos}</p></div>
            <div className="stat-card"><p className="stat-card__label">Faltas</p><p className="stat-card__value">{bi.totalFaltas}</p></div>
            <div className="stat-card"><p className="stat-card__label">Hora extra</p><p className="stat-card__value" style={{ fontSize: 18 }}>{hhmm(bi.totalHoraExtraMinutos)}</p></div>
            <div className="stat-card"><p className="stat-card__label">Sem batida</p><p className="stat-card__value" style={{ color: bi.totalFantasmas > 0 ? 'var(--color-danger)' : 'var(--color-success)' }}>{bi.totalFantasmas}</p></div>
          </div>

          <div className="br-table-wrapper">
            <table className="br-table">
              <caption className="visually-hidden">BI por órgão</caption>
              <thead>
                <tr>
                  <th scope="col">Secretaria / Órgão</th>
                  <th scope="col">Vínculos</th>
                  <th scope="col">Faltas</th>
                  <th scope="col">Hora extra</th>
                  <th scope="col">Sem batida</th>
                </tr>
              </thead>
              <tbody>
                {bi.orgaos.map((o) => (
                  <tr key={o.lotacaoId}>
                    <td>{o.orgao}</td>
                    <td>{o.vinculos}</td>
                    <td>{o.faltas}</td>
                    <td>{hhmm(o.horaExtraMinutos)}</td>
                    <td>{o.fantasmas > 0 ? <span className="br-badge br-badge--danger">{o.fantasmas}</span> : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* Simulador orçamentário + LRF */}
      <div className="br-card" style={{ marginTop: 32 }}>
        <h3 className="br-card__title" style={{ marginBottom: 12 }}>Simulador orçamentário (hora extra × LRF)</h3>
        <div className="form-row" style={{ gap: 12 }}>
          <div className="br-form-group"><label className="br-label" htmlFor="sim-he">Horas extras</label><input id="sim-he" type="number" className="br-input" value={horasExtras} onChange={(e) => setHorasExtras(e.target.value)} /></div>
          <div className="br-form-group"><label className="br-label" htmlFor="sim-ch">Custo/hora (R$)</label><input id="sim-ch" type="number" className="br-input" value={custoHora} onChange={(e) => setCustoHora(e.target.value)} /></div>
        </div>
        <div className="form-row" style={{ gap: 12 }}>
          <div className="br-form-group"><label className="br-label" htmlFor="sim-gp">Gasto c/ pessoal atual (R$)</label><input id="sim-gp" type="number" className="br-input" value={gastoPessoal} onChange={(e) => setGastoPessoal(e.target.value)} /></div>
          <div className="br-form-group"><label className="br-label" htmlFor="sim-rcl">RCL (R$)</label><input id="sim-rcl" type="number" className="br-input" value={rcl} onChange={(e) => setRcl(e.target.value)} /></div>
        </div>
        <button className="br-button br-button--secondary" onClick={() => void simular()}>Simular</button>

        {sim && (
          <div className="stat-grid" style={{ marginTop: 16 }}>
            <div className="stat-card"><p className="stat-card__label">Custo das horas extras</p><p className="stat-card__value" style={{ fontSize: 18 }}>{reais(sim.custoHoraExtraReais)}</p></div>
            <div className="stat-card"><p className="stat-card__label">Gasto projetado</p><p className="stat-card__value" style={{ fontSize: 18 }}>{reais(sim.gastoProjetadoReais)}</p></div>
            <div className="stat-card">
              <p className="stat-card__label">% da RCL (limite {sim.limitePercentual}%)</p>
              <p className="stat-card__value">{sim.percentualRclProjetado}% <span className={`br-badge ${RISCO_BADGE[sim.risco] ?? 'br-badge--neutral'}`}>{sim.risco}</span></p>
            </div>
          </div>
        )}
      </div>

      {/* Painel de ROI / economia (12.3.1) */}
      <div className="br-card" style={{ marginTop: 32 }}>
        <h3 className="br-card__title" style={{ marginBottom: 12 }}>ROI / economia estimada (R$)</h3>
        <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '0 0 12px' }}>
          Combina o apurado da competência (hora extra e faltas) com os percentuais de redução que você espera com o controle de ponto.
        </p>
        <div className="form-row" style={{ gap: 12 }}>
          <div className="br-form-group"><label className="br-label" htmlFor="roi-ch">Custo/hora extra (R$)</label><input id="roi-ch" type="number" className="br-input" value={roiCustoHora} onChange={(e) => setRoiCustoHora(e.target.value)} /></div>
          <div className="br-form-group"><label className="br-label" htmlFor="roi-rhe">Redução hora extra (%)</label><input id="roi-rhe" type="number" className="br-input" value={roiRedHe} onChange={(e) => setRoiRedHe(e.target.value)} /></div>
        </div>
        <div className="form-row" style={{ gap: 12 }}>
          <div className="br-form-group"><label className="br-label" htmlFor="roi-cf">Custo/dia de falta (R$)</label><input id="roi-cf" type="number" className="br-input" value={roiCustoFalta} onChange={(e) => setRoiCustoFalta(e.target.value)} /></div>
          <div className="br-form-group"><label className="br-label" htmlFor="roi-ra">Redução absenteísmo (%)</label><input id="roi-ra" type="number" className="br-input" value={roiRedAbs} onChange={(e) => setRoiRedAbs(e.target.value)} /></div>
        </div>
        <button className="br-button br-button--secondary" onClick={() => void calcularRoi()}>Calcular economia</button>

        {roi && (
          <div className="stat-grid" style={{ marginTop: 16 }}>
            <div className="stat-card"><p className="stat-card__label">Custo hora extra atual</p><p className="stat-card__value" style={{ fontSize: 16 }}>{reais(roi.custoHoraExtraAtualReais)}</p></div>
            <div className="stat-card"><p className="stat-card__label">Economia hora extra</p><p className="stat-card__value" style={{ fontSize: 16, color: 'var(--color-success)' }}>{reais(roi.economiaHoraExtraReais)}</p></div>
            <div className="stat-card"><p className="stat-card__label">Economia absenteísmo</p><p className="stat-card__value" style={{ fontSize: 16, color: 'var(--color-success)' }}>{reais(roi.economiaAbsenteismoReais)}</p></div>
            <div className="stat-card"><p className="stat-card__label">Economia total estimada</p><p className="stat-card__value" style={{ fontSize: 18, color: 'var(--color-success)' }}>{reais(roi.economiaTotalEstimadaReais)}</p></div>
          </div>
        )}
      </div>
    </>
  )
}
