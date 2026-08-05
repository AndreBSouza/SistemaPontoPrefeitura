import { useEffect, useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import type { AdesaoResponse, AdesaoGrupo, ResumoSatisfacao } from '../api/types'

function Barra({ pct }: { pct: number }) {
  const cor = pct >= 80 ? 'var(--color-success)' : pct >= 50 ? 'var(--color-warning, #f0a020)' : 'var(--color-danger)'
  return (
    <div style={{ background: 'var(--color-gray-8, #e6e6e6)', borderRadius: 4, height: 14, width: 160, overflow: 'hidden' }}>
      <div style={{ width: `${pct}%`, height: '100%', background: cor }} />
    </div>
  )
}

function Tabela({ titulo, grupos, colRotulo }: { titulo: string; grupos: AdesaoGrupo[]; colRotulo: string }) {
  return (
    <div className="br-card" style={{ marginBottom: 24 }}>
      <h3 className="br-card__title" style={{ marginBottom: 12 }}>{titulo}</h3>
      <div className="br-table-wrapper">
        <table className="br-table">
          <caption className="visually-hidden">{titulo}</caption>
          <thead>
            <tr>
              <th scope="col">{colRotulo}</th>
              <th scope="col">Vínculos</th>
              <th scope="col">Aderiram</th>
              <th scope="col">Adesão</th>
            </tr>
          </thead>
          <tbody>
            {grupos.map((g) => (
              <tr key={g.chave}>
                <td>{g.rotulo}</td>
                <td>{g.vinculos}</td>
                <td>{g.aderiram}</td>
                <td style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Barra pct={g.percentual} /> <strong>{g.percentual}%</strong>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default function AdesaoPage() {
  const [porOrgao, setPorOrgao] = useState<AdesaoResponse | null>(null)
  const [porRegime, setPorRegime] = useState<AdesaoResponse | null>(null)
  const [satisfacao, setSatisfacao] = useState<ResumoSatisfacao | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)

  useEffect(() => {
    Promise.all([
      api.get<AdesaoResponse>('/api/relatorios/adesao/orgao'),
      api.get<AdesaoResponse>('/api/relatorios/adesao/regime'),
      api.get<ResumoSatisfacao>('/api/satisfacao/resumo'),
    ])
      .then(([o, r, s]) => { setPorOrgao(o); setPorRegime(r); setSatisfacao(s) })
      .catch(setError)
      .finally(() => setLoading(false))
  }, [])

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Adesão' }]} />
      <h1 className="page-title">Adesão ao ponto eletrônico</h1>
      <p className="page-subtitle">Acompanhamento da implantação por órgão (piloto) e isonomia por regime</p>

      {error && <ErrorAlert error={error} />}
      {loading ? <Loading /> : (
        <>
          {porOrgao && <Tabela titulo="Adesão por órgão" grupos={porOrgao.grupos} colRotulo="Órgão" />}
          {porRegime && <Tabela titulo="Isonomia — adesão por regime" grupos={porRegime.grupos} colRotulo="Regime" />}
          {satisfacao && (
            <div className="br-card">
              <h3 className="br-card__title" style={{ marginBottom: 12 }}>Satisfação dos servidores</h3>
              <div className="stat-grid">
                <div className="stat-card"><p className="stat-card__label">Respostas</p><p className="stat-card__value">{satisfacao.total}</p></div>
                <div className="stat-card"><p className="stat-card__label">Média</p><p className="stat-card__value">{satisfacao.media} / 5</p></div>
              </div>
              {satisfacao.total > 0 && (
                <div style={{ marginTop: 8 }}>
                  {[5, 4, 3, 2, 1].map((n) => (
                    <div key={n} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                      <span style={{ width: 60 }}>{n} ★</span>
                      <Barra pct={satisfacao.total > 0 ? Math.round(100 * (satisfacao.distribuicao[String(n)] ?? 0) / satisfacao.total) : 0} />
                      <span>{satisfacao.distribuicao[String(n)] ?? 0}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </>
      )}
    </>
  )
}
