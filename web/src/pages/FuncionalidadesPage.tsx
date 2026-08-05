import { useEffect, useState } from 'react'

import { api, ApiError } from '../api/client'
import { AnomaliaResponse, FuncionalidadeResponse } from '../api/types'

/**
 * Painel do administrador para ligar/desligar funcionalidades opcionais por ente:
 * anomalias heurísticas e os recursos de IA (assistente, OCR de atestado, resumo executivo).
 * A IA só responde de fato se, além de ligada aqui, houver um provedor configurado no ambiente.
 */

const DESCRICOES: Record<string, string> = {
  ANOMALIAS:
    'Cruza a hora extra de cada servidor com a média do ente e sinaliza os casos muito acima do padrão. Heurística explicável, sem IA — funciona já.',
  IA_ASSISTENTE:
    'Assistente/chatbot no app do servidor (ex.: "quanto de banco de horas eu tenho?"). Requer provedor de IA no ambiente.',
  IA_OCR:
    'Lê a foto do atestado médico e pré-preenche a justificativa (CID, início, dias). Requer provedor de IA no ambiente.',
  IA_RESUMO:
    'Gera um resumo executivo do mês em linguagem natural a partir dos indicadores. Requer provedor de IA no ambiente.',
  IA_SENTIMENTO:
    'Classifica os comentários da pesquisa de satisfação (positivo/neutro/negativo) e resume os temas. Requer provedor de IA no ambiente.',
}

function mesAtual(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

export default function FuncionalidadesPage() {
  const [flags, setFlags] = useState<FuncionalidadeResponse[]>([])
  const [carregando, setCarregando] = useState(true)
  const [salvando, setSalvando] = useState<string | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  // Demo ao vivo das anomalias (aparece quando ANOMALIAS está ligada).
  const [competencia, setCompetencia] = useState(mesAtual())
  const [anomalias, setAnomalias] = useState<AnomaliaResponse | null>(null)
  const [buscandoAnom, setBuscandoAnom] = useState(false)

  const anomaliasLigada = flags.find((f) => f.chave === 'ANOMALIAS')?.habilitado ?? false

  useEffect(() => {
    api
      .get<FuncionalidadeResponse[]>('/api/funcionalidades')
      .then(setFlags)
      .catch((e) => setErro(e instanceof ApiError ? e.message : String(e)))
      .finally(() => setCarregando(false))
  }, [])

  async function alternar(chave: string, habilitado: boolean) {
    setSalvando(chave)
    setErro(null)
    try {
      await api.put<void>(`/api/funcionalidades/${chave}?habilitado=${habilitado}`)
      setFlags((atual) => atual.map((f) => (f.chave === chave ? { ...f, habilitado } : f)))
      if (chave === 'ANOMALIAS' && !habilitado) setAnomalias(null)
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : String(e))
    } finally {
      setSalvando(null)
    }
  }

  async function verAnomalias() {
    setBuscandoAnom(true)
    setErro(null)
    try {
      const r = await api.get<AnomaliaResponse>(`/api/relatorios/anomalias?competencia=${competencia}`)
      setAnomalias(r)
    } catch (e) {
      setErro(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBuscandoAnom(false)
    }
  }

  if (carregando) return <p>Carregando…</p>

  return (
    <>
      <h1 className="page-title">Funcionalidades</h1>
      <p className="page-subtitle">
        Ligue ou desligue recursos opcionais para a sua prefeitura. Tudo começa desligado — nada roda sem a sua decisão.
      </p>

      {erro && <div className="br-alert br-alert--danger" role="alert" style={{ marginBottom: 12 }}>{erro}</div>}

      <div className="br-card" style={{ display: 'grid', gap: 4 }}>
        {flags.map((f) => (
          <div
            key={f.chave}
            style={{
              display: 'flex',
              gap: 16,
              alignItems: 'flex-start',
              justifyContent: 'space-between',
              padding: '14px 4px',
              borderBottom: '1px solid var(--color-gray-8, #eee)',
            }}
          >
            <div style={{ maxWidth: 620 }}>
              <strong>{f.rotulo}</strong>
              <p style={{ margin: '4px 0 0', color: 'var(--color-gray-60, #666)', fontSize: 14 }}>
                {DESCRICOES[f.chave] ?? ''}
              </p>
            </div>
            <label style={{ display: 'inline-flex', gap: 8, alignItems: 'center', whiteSpace: 'nowrap', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={f.habilitado}
                disabled={salvando === f.chave}
                onChange={(e) => void alternar(f.chave, e.target.checked)}
                style={{ width: 20, height: 20 }}
              />
              <span style={{ color: f.habilitado ? 'var(--color-success, #168821)' : 'var(--color-gray-60, #888)', fontWeight: 700 }}>
                {f.habilitado ? 'Ligada' : 'Desligada'}
              </span>
            </label>
          </div>
        ))}
      </div>

      {anomaliasLigada && (
        <div className="br-card" style={{ marginTop: 24 }}>
          <h2 style={{ marginTop: 0, fontSize: 18 }}>Anomalias — hora extra atípica</h2>
          <p style={{ color: 'var(--color-gray-60, #666)', fontSize: 14, marginTop: 4 }}>
            Servidores com hora extra muito acima da média do ente no mês. Para revisão do RH/controladoria — não é acusação.
          </p>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', margin: '12px 0' }}>
            <input
              type="month"
              className="br-input"
              value={competencia}
              onChange={(e) => setCompetencia(e.target.value)}
              style={{ maxWidth: 180 }}
            />
            <button className="br-button br-button--primary" onClick={() => void verAnomalias()} disabled={buscandoAnom}>
              {buscandoAnom ? 'Analisando…' : 'Analisar mês'}
            </button>
          </div>

          {anomalias && anomalias.anomalias.length === 0 && (
            <div className="br-alert br-alert--success" role="status">Nenhuma anomalia no período. 👍</div>
          )}
          {anomalias && anomalias.anomalias.length > 0 && (
            <table className="br-table" style={{ width: '100%' }}>
              <thead>
                <tr>
                  <th>Servidor</th>
                  <th>Sinal</th>
                </tr>
              </thead>
              <tbody>
                {anomalias.anomalias.map((a) => (
                  <tr key={a.vinculoId}>
                    <td>{a.servidor}</td>
                    <td>{a.detalhe}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </>
  )
}
