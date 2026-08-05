import { useEffect, useState } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { api } from '../api/client'
import { useTenant } from '../contexts/TenantContext'
import { podeAcessar } from '../auth/roles'
import type { ServidorResponse, VinculoResponse, LotacaoResponse } from '../api/types'
import { CriarServidorModal } from './servidores/CriarServidorModal'
import { ImportarCSVModal } from './servidores/ImportarCSVModal'
import { GerarCodigoModal } from './servidores/GerarCodigoModal'
import { DispositivosModal } from './servidores/DispositivosModal'
import { SobreavisoModal } from './servidores/SobreavisoModal'
import { RegistrosLocalizacaoModal } from './servidores/RegistrosLocalizacaoModal'
import { LotarModal } from './servidores/LotarModal'

export default function ServidoresPage() {
  const { roles } = useTenant()
  // Espelha o SecurityConfig: só mostra a ação que o papel consegue concluir (senão o modal abre
  // direto em 403). Ex.: controladoria enxerga a lista, mas não ativa aparelho nem lança sobreaviso.
  const podeAtivar = podeAcessar(roles, ['rh', 'tenant-admin'])
  const podeSobreaviso = podeAcessar(roles, ['gestor', 'rh', 'tenant-admin'])

  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [showCriar, setShowCriar] = useState(false)
  const [showImportar, setShowImportar] = useState(false)
  const [sucesso, setSucesso] = useState('')
  const [gerarCodigoVinculoId, setGerarCodigoVinculoId] = useState<string | null>(null)
  const [dispositivosVinculoId, setDispositivosVinculoId] = useState<string | null>(null)
  const [registrosVinculoId, setRegistrosVinculoId] = useState<string | null>(null)
  const [sobreavisoVinculoId, setSobreavisoVinculoId] = useState<string | null>(null)
  const [lotarVinculo, setLotarVinculo] = useState<VinculoResponse | null>(null)
  const [lotacoes, setLotacoes] = useState<LotacaoResponse[]>([])

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [srv, lot] = await Promise.all([
        api.get<ServidorResponse[]>('/api/servidores'),
        api.get<LotacaoResponse[]>('/api/lotacoes'),
      ])
      setServidores(srv)
      setLotacoes(lot)
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

  const lotacaoNome = (id: string | null) =>
    id ? (lotacoes.find((l) => l.id === id)?.nome ?? '—') : '—'

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Servidores' }]} />
      <h1 className="page-title">Servidores</h1>
      <p className="page-subtitle">Cadastro de servidores e vínculos empregatícios</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <div className="action-bar">
        <div className="action-bar__left">
          <span>{servidores.length} servidor(es)</span>
        </div>
        <div className="action-bar__right">
          <button className="br-button br-button--secondary" onClick={() => setShowImportar(true)}>
            📥 Importar CSV
          </button>
          <button className="br-button br-button--primary" onClick={() => setShowCriar(true)}>
            + Novo Servidor
          </button>
        </div>
      </div>

      {loading ? (
        <Loading />
      ) : servidores.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">👥</div>
          <p className="empty-state__title">Nenhum servidor cadastrado</p>
          <p>Clique em "Novo Servidor" ou importe via CSV.</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Lista de servidores</caption>
            <thead>
              <tr>
                <th scope="col">Nome</th>
                <th scope="col">CPF</th>
                <th scope="col">E-mail</th>
                <th scope="col">Vínculos</th>
                <th scope="col">Órgão</th>
                <th scope="col">Situação</th>
                <th scope="col">App Móvel</th>
              </tr>
            </thead>
            <tbody>
              {servidores.map((s) => (
                <tr key={s.id}>
                  <td>{s.nome}</td>
                  <td style={{ fontFamily: 'monospace' }}>{s.cpf}</td>
                  <td>{s.email ?? '—'}</td>
                  <td>
                    {s.vinculos.map((v) => (
                      <span key={v.id} style={{ display: 'block', fontSize: 12 }}>
                        {v.matricula} / {v.regime}
                        {v.cargo ? ` — ${v.cargo}` : ''}
                      </span>
                    ))}
                  </td>
                  <td>
                    {s.vinculos.map((v) => (
                      <span key={v.id} style={{ display: 'block', fontSize: 12 }}>
                        {lotacaoNome(v.lotacaoId)}
                      </span>
                    ))}
                  </td>
                  <td>
                    {s.ativo ? (
                      <span className="br-badge br-badge--success">Ativo</span>
                    ) : (
                      <span className="br-badge br-badge--neutral">Inativo</span>
                    )}
                  </td>
                  <td>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                      {s.vinculos.map((v) => (
                        <div key={v.id} style={{ display: 'flex', gap: 4, alignItems: 'center', flexWrap: 'wrap' }}>
                          <span style={{ fontSize: 11, color: 'var(--color-gray-60)', minWidth: 80 }}>
                            {v.matricula}
                          </span>
                          {podeAtivar && (
                            <>
                              <button
                                className="br-button br-button--sm br-button--secondary"
                                title={`Gerar código de ativação para ${v.matricula}`}
                                aria-label={`Gerar código de ativação para vínculo ${v.matricula}`}
                                onClick={() => setGerarCodigoVinculoId(v.id)}
                              >
                                📲 Ativar
                              </button>
                              <button
                                className="br-button br-button--sm br-button--tertiary"
                                title={`Gerenciar dispositivos de ${v.matricula}`}
                                aria-label={`Gerenciar dispositivos do vínculo ${v.matricula}`}
                                onClick={() => setDispositivosVinculoId(v.id)}
                              >
                                📱 Dispositivos
                              </button>
                            </>
                          )}
                          <button
                            className="br-button br-button--sm br-button--tertiary"
                            title={`Registros e localização de ${v.matricula}`}
                            aria-label={`Registros e localização do vínculo ${v.matricula}`}
                            onClick={() => setRegistrosVinculoId(v.id)}
                          >
                            📍 Localização
                          </button>
                          {podeSobreaviso && (
                            <button
                              className="br-button br-button--sm br-button--tertiary"
                              title={`Sobreaviso de ${v.matricula}`}
                              aria-label={`Sobreaviso do vínculo ${v.matricula}`}
                              onClick={() => setSobreavisoVinculoId(v.id)}
                            >
                              ⏰ Sobreaviso
                            </button>
                          )}
                          <button
                            className="br-button br-button--sm br-button--tertiary"
                            title={`Lotar ${v.matricula} em um órgão`}
                            aria-label={`Lotar vínculo ${v.matricula} em um órgão`}
                            onClick={() => setLotarVinculo(v)}
                          >
                            🏛️ Órgão
                          </button>
                        </div>
                      ))}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCriar && (
        <CriarServidorModal
          lotacoes={lotacoes}
          onClose={() => setShowCriar(false)}
          onSaved={(msg) => { setShowCriar(false); void load(); flash(msg) }}
        />
      )}

      {lotarVinculo && (
        <LotarModal
          vinculo={lotarVinculo}
          lotacoes={lotacoes}
          onClose={() => setLotarVinculo(null)}
          onSaved={(msg) => { setLotarVinculo(null); void load(); flash(msg) }}
        />
      )}

      {showImportar && (
        <ImportarCSVModal
          onClose={() => setShowImportar(false)}
          onDone={(msg) => { setShowImportar(false); void load(); flash(msg) }}
        />
      )}

      {gerarCodigoVinculoId && (
        <GerarCodigoModal
          vinculoId={gerarCodigoVinculoId}
          onClose={() => setGerarCodigoVinculoId(null)}
        />
      )}

      {dispositivosVinculoId && (
        <DispositivosModal
          vinculoId={dispositivosVinculoId}
          onClose={() => setDispositivosVinculoId(null)}
        />
      )}

      {registrosVinculoId && (
        <RegistrosLocalizacaoModal
          vinculoId={registrosVinculoId}
          onClose={() => setRegistrosVinculoId(null)}
        />
      )}

      {sobreavisoVinculoId && (
        <SobreavisoModal
          vinculoId={sobreavisoVinculoId}
          onClose={() => setSobreavisoVinculoId(null)}
        />
      )}
    </>
  )
}
