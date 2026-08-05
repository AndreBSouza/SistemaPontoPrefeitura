import { useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type {
  ConformidadeResponse,
  DeteccaoResponse,
  AlertasResponse,
  InconsistenciasResponse,
  AbonoResponse,
  DossieResponse,
  PisoMagisterio,
} from '../api/types'

export default function ConformidadePage() {
  const [competencia, setCompetencia] = useState(() => {
    const now = new Date()
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  })
  const [resultado, setResultado] = useState<ConformidadeResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [deteccao, setDeteccao] = useState<DeteccaoResponse | null>(null)
  const [loadingDet, setLoadingDet] = useState(false)
  const [alertas, setAlertas] = useState<AlertasResponse | null>(null)
  const [loadingAlertas, setLoadingAlertas] = useState(false)
  const [inconsistencias, setInconsistencias] = useState<InconsistenciasResponse | null>(null)
  const [abonos, setAbonos] = useState<AbonoResponse[] | null>(null)
  const [piso, setPiso] = useState<PisoMagisterio[] | null>(null)
  const [dossie, setDossie] = useState<DossieResponse | null>(null)

  async function verificar() {
    setLoading(true)
    setError(null)
    try {
      setResultado(await api.get<ConformidadeResponse>(`/api/conformidade/in008?competencia=${competencia}`))
    } catch (e) {
      setError(e)
    } finally {
      setLoading(false)
    }
  }

  async function detectar() {
    setLoadingDet(true)
    setError(null)
    try {
      setDeteccao(await api.get<DeteccaoResponse>(`/api/relatorios/acumulo?competencia=${competencia}`))
    } catch (e) {
      setError(e)
    } finally {
      setLoadingDet(false)
    }
  }

  async function carregarDossie() {
    setError(null)
    try {
      setDossie(await api.get<DossieResponse>(`/api/relatorios/dossie?competencia=${competencia}`))
    } catch (e) {
      setError(e)
    }
  }

  async function carregarInconsistencias() {
    setError(null)
    try {
      setInconsistencias(await api.get<InconsistenciasResponse>(`/api/relatorios/inconsistencias?competencia=${competencia}`))
    } catch (e) {
      setError(e)
    }
  }

  async function carregarPiso() {
    setError(null)
    try {
      setPiso(await api.get<PisoMagisterio[]>('/api/jornadas/piso-magisterio'))
    } catch (e) {
      setError(e)
    }
  }

  async function carregarAbonos() {
    setError(null)
    try {
      setAbonos(await api.get<AbonoResponse[]>(`/api/relatorios/abonos?competencia=${competencia}`))
    } catch (e) {
      setError(e)
    }
  }

  async function baixarAbonosCsv() {
    setError(null)
    try {
      const csv = await api.get<string>(`/api/relatorios/abonos/csv?competencia=${competencia}`)
      const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv' }))
      const a = document.createElement('a')
      a.href = url
      a.download = `abonos-${competencia}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setError(e)
    }
  }

  async function carregarAlertas() {
    setLoadingAlertas(true)
    setError(null)
    try {
      setAlertas(await api.get<AlertasResponse>(`/api/relatorios/alertas?competencia=${competencia}`))
    } catch (e) {
      setError(e)
    } finally {
      setLoadingAlertas(false)
    }
  }

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Conformidade IN-008' }]} />
      <h1 className="page-title">Conformidade IN-008</h1>
      <p className="page-subtitle">Verificação de conformidade com a Instrução Normativa IN-008/MPOG</p>

      {error && <ErrorAlert error={error} />}

      <div className="br-card" style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
          <div className="br-form-group" style={{ marginBottom: 0 }}>
            <label className="br-label" htmlFor="comp-conf">Competência</label>
            <input id="comp-conf" type="month" className="br-input" value={competencia} onChange={(e) => setCompetencia(e.target.value)} />
          </div>
          <button className="br-button br-button--primary" onClick={() => void verificar()} disabled={loading}>
            Verificar
          </button>
          <button className="br-button br-button--secondary" onClick={() => void carregarDossie()}>
            Dossiê de defesa (TCM)
          </button>
        </div>
      </div>

      {dossie && (
        <div className="br-card" style={{ marginBottom: 24, borderLeft: '4px solid var(--color-primary)' }}>
          <h3 className="br-card__title" style={{ marginBottom: 8 }}>Dossiê de conformidade — escudo jurídico</h3>
          <div className="stat-grid">
            <div className="stat-card"><p className="stat-card__label">Cadeia íntegra</p><p className="stat-card__value">{dossie.cadeiaIntegra ? <span className="br-badge br-badge--success">Sim</span> : <span className="br-badge br-badge--danger">Não</span>}</p></div>
            <div className="stat-card"><p className="stat-card__label">Registros (AFD)</p><p className="stat-card__value">{dossie.afdTotalRegistros}</p></div>
            <div className="stat-card"><p className="stat-card__label">Prazo TCM</p><p className="stat-card__value" style={{ fontSize: 15 }}>{dossie.prazoSubmissao}</p></div>
            <div className="stat-card"><p className="stat-card__label">Dias restantes</p><p className="stat-card__value" style={{ color: dossie.diasRestantes < 0 ? 'var(--color-danger)' : 'inherit' }}>{dossie.diasRestantes}</p></div>
          </div>
          <p style={{ fontSize: 12, margin: '8px 0 4px', color: 'var(--color-gray-60)' }}>AFD SHA-256: <code style={{ fontSize: 11 }}>{dossie.afdHashSha256.slice(0, 24)}…</code></p>
          <ul style={{ margin: '8px 0 0', paddingLeft: 18 }}>
            {dossie.escudos.map((e, i) => <li key={i} style={{ fontSize: 13, marginBottom: 2 }}>✔️ {e}</li>)}
          </ul>
        </div>
      )}

      {loading && <Loading />}

      {resultado && (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <p className="stat-card__label">Total servidores</p>
              <p className="stat-card__value">{resultado.totalServidores}</p>
            </div>
            <div className="stat-card">
              <p className="stat-card__label">Total vínculos</p>
              <p className="stat-card__value">{resultado.totalVinculos}</p>
            </div>
            <div className="stat-card">
              <p className="stat-card__label">Total registros</p>
              <p className="stat-card__value">{resultado.totalRegistros}</p>
            </div>
          </div>
          <div className="br-card" style={{ marginTop: 16 }}>
            <h3 className="br-card__title" style={{ marginBottom: 8 }}>Descrição</h3>
            <p>{resultado.descricao}</p>
            <p style={{ fontSize: 12, color: 'var(--color-gray-60)' }}>Competência: {resultado.competencia}</p>
          </div>
        </>
      )}

      {/* Varredura de irregularidades (12.5.2): acúmulo de cargos + servidor fantasma */}
      <div className="br-card" style={{ marginTop: 32 }}>
        <div className="action-bar" style={{ marginTop: 0 }}>
          <div className="action-bar__left">
            <h3 className="br-card__title" style={{ margin: 0 }}>Detecção de irregularidades</h3>
          </div>
          <div className="action-bar__right">
            <button className="br-button br-button--secondary br-button--sm" onClick={() => void detectar()} disabled={loadingDet}>
              {loadingDet ? 'Analisando…' : 'Analisar acúmulo / fantasmas'}
            </button>
          </div>
        </div>
        <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '0 0 12px' }}>
          Cruza jornadas sobrepostas (acúmulo ilícito de cargos) e aponta vínculos ativos sem nenhuma
          batida no período (possíveis "servidores fantasma"). Sem biometria — insumo de controle interno.
        </p>

        {deteccao && (
          <>
            <h4 style={{ fontSize: 14, margin: '12px 0 6px' }}>
              Acúmulo de cargos (jornadas sobrepostas)
            </h4>
            {deteccao.acumulos.length === 0 ? (
              <p style={{ fontSize: 13, color: 'var(--color-gray-60)' }}>Nenhum acúmulo detectado.</p>
            ) : (
              <div className="br-table-wrapper">
                <table className="br-table">
                  <caption className="visually-hidden">Acúmulos detectados</caption>
                  <thead>
                    <tr>
                      <th scope="col">Servidor</th>
                      <th scope="col">Vínculo A</th>
                      <th scope="col">Vínculo B</th>
                    </tr>
                  </thead>
                  <tbody>
                    {deteccao.acumulos.map((a, idx) => (
                      <tr key={idx}>
                        <td>{a.servidor}</td>
                        <td style={{ fontSize: 12, fontFamily: 'monospace' }}>{a.vinculoA}</td>
                        <td style={{ fontSize: 12, fontFamily: 'monospace' }}>{a.vinculoB}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <h4 style={{ fontSize: 14, margin: '16px 0 6px' }}>
              Vínculos sem batida no período
            </h4>
            {deteccao.fantasmas.length === 0 ? (
              <p style={{ fontSize: 13, color: 'var(--color-gray-60)' }}>Nenhum vínculo sem batida.</p>
            ) : (
              <div className="br-table-wrapper">
                <table className="br-table">
                  <caption className="visually-hidden">Vínculos sem batida</caption>
                  <thead>
                    <tr>
                      <th scope="col">Servidor</th>
                      <th scope="col">Matrícula</th>
                    </tr>
                  </thead>
                  <tbody>
                    {deteccao.fantasmas.map((f, idx) => (
                      <tr key={idx}>
                        <td>{f.servidor}</td>
                        <td>{f.matricula}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>

      {/* Alertas de risco proativos (12.6.10) */}
      <div className="br-card" style={{ marginTop: 32 }}>
        <div className="action-bar" style={{ marginTop: 0 }}>
          <div className="action-bar__left">
            <h3 className="br-card__title" style={{ margin: 0 }}>Alertas de risco</h3>
          </div>
          <div className="action-bar__right">
            <button className="br-button br-button--secondary br-button--sm" onClick={() => void carregarAlertas()} disabled={loadingAlertas}>
              {loadingAlertas ? 'Carregando…' : 'Carregar alertas'}
            </button>
          </div>
        </div>
        <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '0 0 12px' }}>
          Ajustes manuais de banco de horas (vetor de abuso) e batidas marcadas fora da cerca no período.
        </p>

        {alertas && (
          <>
            <h4 style={{ fontSize: 14, margin: '12px 0 6px' }}>Ajustes manuais de banco de horas</h4>
            {alertas.ajustesManuais.length === 0 ? (
              <p style={{ fontSize: 13, color: 'var(--color-gray-60)' }}>Nenhum ajuste manual no período.</p>
            ) : (
              <div className="br-table-wrapper">
                <table className="br-table">
                  <caption className="visually-hidden">Ajustes manuais</caption>
                  <thead>
                    <tr>
                      <th scope="col">Servidor</th>
                      <th scope="col">Data</th>
                      <th scope="col">Minutos</th>
                      <th scope="col">Descrição</th>
                    </tr>
                  </thead>
                  <tbody>
                    {alertas.ajustesManuais.map((a, idx) => (
                      <tr key={idx}>
                        <td>{a.servidor}</td>
                        <td>{a.data}</td>
                        <td>{a.minutos}</td>
                        <td>{a.descricao}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <h4 style={{ fontSize: 14, margin: '16px 0 6px' }}>Batidas fora da cerca</h4>
            {alertas.batidasForaDaCerca.length === 0 ? (
              <p style={{ fontSize: 13, color: 'var(--color-gray-60)' }}>Nenhuma batida fora da cerca.</p>
            ) : (
              <div className="br-table-wrapper">
                <table className="br-table">
                  <caption className="visually-hidden">Batidas fora da cerca</caption>
                  <thead>
                    <tr>
                      <th scope="col">Servidor</th>
                      <th scope="col">Quantidade</th>
                    </tr>
                  </thead>
                  <tbody>
                    {alertas.batidasForaDaCerca.map((f, idx) => (
                      <tr key={idx}>
                        <td>{f.servidor}</td>
                        <td>{f.quantidade}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>

      {/* Inconsistências de jornada (12.4.15) */}
      <div className="br-card" style={{ marginTop: 32 }}>
        <div className="action-bar" style={{ marginTop: 0 }}>
          <div className="action-bar__left">
            <h3 className="br-card__title" style={{ margin: 0 }}>Inconsistências de jornada</h3>
          </div>
          <div className="action-bar__right">
            <button className="br-button br-button--secondary br-button--sm" onClick={() => void carregarInconsistencias()}>Verificar</button>
          </div>
        </div>
        <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '0 0 12px' }}>
          Dias com número ímpar de marcações (intervalo aberto — possível esquecimento de batida).
        </p>
        {inconsistencias && (
          inconsistencias.inconsistencias.length === 0 ? (
            <p style={{ fontSize: 13, color: 'var(--color-gray-60)' }}>Nenhuma inconsistência no período.</p>
          ) : (
            <div className="br-table-wrapper">
              <table className="br-table">
                <caption className="visually-hidden">Inconsistências</caption>
                <thead>
                  <tr><th scope="col">Servidor</th><th scope="col">Data</th><th scope="col">Marcações</th></tr>
                </thead>
                <tbody>
                  {inconsistencias.inconsistencias.map((i, idx) => (
                    <tr key={idx}><td>{i.servidor}</td><td>{i.data}</td><td>{i.marcacoes}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}
      </div>

      {/* Hora-atividade do magistério — Lei do Piso (12.5.8) */}
      <div className="br-card" style={{ marginTop: 24 }}>
        <div className="action-bar" style={{ marginTop: 0 }}>
          <div className="action-bar__left">
            <h3 className="br-card__title" style={{ margin: 0 }}>Hora-atividade (Lei do Piso)</h3>
          </div>
          <div className="action-bar__right">
            <button className="br-button br-button--secondary br-button--sm" onClick={() => void carregarPiso()}>Verificar</button>
          </div>
        </div>
        <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '0 0 12px' }}>
          Jornadas com hora-atividade declarada. O mínimo legal do magistério é 1/3 da carga.
        </p>
        {piso && (
          piso.length === 0 ? (
            <p style={{ fontSize: 13, color: 'var(--color-gray-60)' }}>Nenhuma jornada com hora-atividade declarada.</p>
          ) : (
            <div className="br-table-wrapper">
              <table className="br-table">
                <caption className="visually-hidden">Hora-atividade</caption>
                <thead>
                  <tr><th scope="col">Jornada</th><th scope="col">Carga (min)</th><th scope="col">Hora-atividade</th><th scope="col">Mínimo (1/3)</th><th scope="col">%</th><th scope="col">Situação</th></tr>
                </thead>
                <tbody>
                  {piso.map((p) => (
                    <tr key={p.jornadaId}>
                      <td>{p.nome}</td>
                      <td>{p.cargaHorariaSemanalMin}</td>
                      <td>{p.horaAtividadeMin}</td>
                      <td>{p.minimoLegalMin}</td>
                      <td>{(p.percentual * 100).toFixed(1)}%</td>
                      <td>{p.atendePiso
                        ? <span className="br-badge br-badge--success">Atende</span>
                        : <span className="br-badge br-badge--danger">Abaixo do mínimo</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}
      </div>

      {/* Relatório de abonos e exceções (12.6.15) */}
      <div className="br-card" style={{ marginTop: 24 }}>
        <div className="action-bar" style={{ marginTop: 0 }}>
          <div className="action-bar__left">
            <h3 className="br-card__title" style={{ margin: 0 }}>Abonos e exceções</h3>
          </div>
          <div className="action-bar__right" style={{ display: 'flex', gap: 6 }}>
            <button className="br-button br-button--secondary br-button--sm" onClick={() => void carregarAbonos()}>Listar</button>
            <button className="br-button br-button--tertiary br-button--sm" onClick={() => void baixarAbonosCsv()}>Exportar CSV</button>
          </div>
        </div>
        {abonos && (
          abonos.length === 0 ? (
            <p style={{ fontSize: 13, color: 'var(--color-gray-60)' }}>Nenhum abono no período.</p>
          ) : (
            <div className="br-table-wrapper">
              <table className="br-table">
                <caption className="visually-hidden">Abonos</caption>
                <thead>
                  <tr><th scope="col">Servidor</th><th scope="col">Tipo</th><th scope="col">Período</th><th scope="col">Status</th><th scope="col">Decisão</th></tr>
                </thead>
                <tbody>
                  {abonos.map((a) => (
                    <tr key={a.id}>
                      <td>{a.servidor}</td>
                      <td>{a.tipo}</td>
                      <td>{a.dataInicio} a {a.dataFim}</td>
                      <td>{a.status}</td>
                      <td>{a.motivoDecisao ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        )}
      </div>
    </>
  )
}
