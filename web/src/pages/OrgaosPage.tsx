import { useEffect, useState, FormEvent } from 'react'
import { Breadcrumb } from '../components/Breadcrumb'
import { Alert, ErrorAlert } from '../components/Alert'
import { Loading } from '../components/Loading'
import { Modal } from '../components/Modal'
import { api } from '../api/client'
import type {
  LotacaoResponse,
  CriarLotacaoRequest,
  DefinirRegrasRequest,
  JornadaResponse,
  ServidorResponse,
  GeofenceLocal,
  CriarGeofenceLocalRequest,
} from '../api/types'

export default function OrgaosPage() {
  const [lotacoes, setLotacoes] = useState<LotacaoResponse[]>([])
  const [jornadas, setJornadas] = useState<JornadaResponse[]>([])
  const [servidores, setServidores] = useState<ServidorResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)

  const [showCriar, setShowCriar] = useState(false)
  const [showRegras, setShowRegras] = useState<LotacaoResponse | null>(null)
  const [showChefia, setShowChefia] = useState<LotacaoResponse | null>(null)
  const [showLocais, setShowLocais] = useState<LotacaoResponse | null>(null)
  const [sucesso, setSucesso] = useState('')

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [l, j, s] = await Promise.all([
        api.get<LotacaoResponse[]>('/api/lotacoes'),
        api.get<JornadaResponse[]>('/api/jornadas'),
        api.get<ServidorResponse[]>('/api/servidores'),
      ])
      setLotacoes(l)
      setJornadas(j)
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

  return (
    <>
      <Breadcrumb items={[{ label: 'Dashboard', to: '/dashboard' }, { label: 'Órgãos / Lotações' }]} />
      <h1 className="page-title">Órgãos / Lotações</h1>
      <p className="page-subtitle">Gerencie lotações, regras de ponto e chefia</p>

      {sucesso && <Alert variant="success">{sucesso}</Alert>}
      {error && <ErrorAlert error={error} />}

      <div className="action-bar">
        <div className="action-bar__left">
          <span>{lotacoes.length} lotação(ões) cadastrada(s)</span>
        </div>
        <div className="action-bar__right">
          <button className="br-button br-button--primary" onClick={() => setShowCriar(true)}>
            + Nova Lotação
          </button>
        </div>
      </div>

      {loading ? (
        <Loading />
      ) : lotacoes.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state__icon" aria-hidden="true">🏛️</div>
          <p className="empty-state__title">Nenhuma lotação cadastrada</p>
          <p>Clique em "Nova Lotação" para começar.</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Lista de lotações</caption>
            <thead>
              <tr>
                <th scope="col">Nome</th>
                <th scope="col">Sigla</th>
                <th scope="col">Banco Horas</th>
                <th scope="col">Tolerância (min)</th>
                <th scope="col">Geofence</th>
                <th scope="col">Ações</th>
              </tr>
            </thead>
            <tbody>
              {lotacoes.map((l) => (
                <tr key={l.id}>
                  <td>{l.nome}</td>
                  <td>{l.sigla ?? '—'}</td>
                  <td>
                    {l.bancoHorasHabilitado === null ? '—' :
                     l.bancoHorasHabilitado ? (
                       <span className="br-badge br-badge--success">Ativo</span>
                     ) : (
                       <span className="br-badge br-badge--neutral">Inativo</span>
                     )}
                  </td>
                  <td>{l.toleranciaMinutos ?? '—'}</td>
                  <td>
                    {l.geofenceRaioMetros ? (
                      <span className="br-badge br-badge--info">{l.geofenceRaioMetros}m</span>
                    ) : '—'}
                  </td>
                  <td>
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button
                        className="br-button br-button--secondary br-button--sm"
                        onClick={() => setShowRegras(l)}
                      >
                        Regras
                      </button>
                      <button
                        className="br-button br-button--tertiary br-button--sm"
                        onClick={() => setShowChefia(l)}
                      >
                        Chefia
                      </button>
                      <button
                        className="br-button br-button--tertiary br-button--sm"
                        onClick={() => setShowLocais(l)}
                      >
                        📍 Locais
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modal: Criar lotação */}
      {showCriar && (
        <CriarLotacaoModal
          onClose={() => setShowCriar(false)}
          onSaved={(msg) => { setShowCriar(false); void load(); flash(msg) }}
        />
      )}

      {/* Modal: Regras de ponto */}
      {showRegras && (
        <RegrasModal
          lotacao={showRegras}
          jornadas={jornadas}
          onClose={() => setShowRegras(null)}
          onSaved={(msg) => { setShowRegras(null); void load(); flash(msg) }}
        />
      )}

      {/* Modal: Definir chefia */}
      {showChefia && (
        <ChefiaModal
          lotacao={showChefia}
          servidores={servidores}
          onClose={() => setShowChefia(null)}
          onSaved={(msg) => { setShowChefia(null); void load(); flash(msg) }}
        />
      )}

      {/* Modal: Locais (áreas adicionais / multi-geofence) */}
      {showLocais && (
        <LocaisModal
          lotacao={showLocais}
          onClose={() => setShowLocais(null)}
        />
      )}
    </>
  )
}

// ---- Locais adicionais (multi-geofence / áreas volantes) ----
function LocaisModal({ lotacao, onClose }: {
  lotacao: LotacaoResponse
  onClose: () => void
}) {
  const [locais, setLocais] = useState<GeofenceLocal[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [saving, setSaving] = useState(false)
  const [nome, setNome] = useState('')
  const [lat, setLat] = useState('')
  const [lon, setLon] = useState('')
  const [raio, setRaio] = useState('')

  function load() {
    setLoading(true)
    api
      .get<GeofenceLocal[]>(`/api/lotacoes/${lotacao.id}/locais`)
      .then(setLocais)
      .catch(setError)
      .finally(() => setLoading(false))
  }
  useEffect(load, [lotacao.id])

  async function adicionar(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body: CriarGeofenceLocalRequest = {
        nome,
        latitude: Number(lat),
        longitude: Number(lon),
        raioMetros: Number(raio),
      }
      await api.post<GeofenceLocal>(`/api/lotacoes/${lotacao.id}/locais`, body)
      setNome(''); setLat(''); setLon(''); setRaio('')
      load()
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function remover(id: string) {
    setError(null)
    try {
      await api.delete<void>(`/api/lotacoes/${lotacao.id}/locais/${id}`)
      load()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <Modal title={`Locais de referência — ${lotacao.nome}`} onClose={onClose}>
      {error != null && <ErrorAlert error={error} />}
      <Alert variant="info">
        Várias áreas para órgãos com locais volantes (postos, frentes de trabalho). A batida fica
        "fora da área" só quando está fora de <strong>todas</strong> (incluindo a área principal das
        Regras). Serve só para o administrador conferir — <strong>não bloqueia nem avisa o servidor</strong>.
      </Alert>

      <form onSubmit={adicionar} noValidate style={{ marginBottom: 16 }}>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="loc-nome">Nome do local</label>
          <input id="loc-nome" className="br-input" value={nome} onChange={(e) => setNome(e.target.value)}
                 required placeholder="Ex.: Posto Central" />
        </div>
        <div className="form-row-3">
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="loc-lat">Latitude</label>
            <input id="loc-lat" type="number" step="any" className="br-input" value={lat}
                   onChange={(e) => setLat(e.target.value)} required placeholder="-23.5505" />
          </div>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="loc-lon">Longitude</label>
            <input id="loc-lon" type="number" step="any" className="br-input" value={lon}
                   onChange={(e) => setLon(e.target.value)} required placeholder="-46.6333" />
          </div>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="loc-raio">Raio (m)</label>
            <input id="loc-raio" type="number" min="1" className="br-input" value={raio}
                   onChange={(e) => setRaio(e.target.value)} required placeholder="100" />
          </div>
        </div>
        <button className="br-button br-button--secondary" type="submit" disabled={saving}>
          {saving ? 'Adicionando…' : 'Adicionar local'}
        </button>
      </form>

      {loading ? (
        <Loading label="Carregando locais…" />
      ) : locais.length === 0 ? (
        <div className="empty-state">
          <p className="empty-state__title">Nenhum local adicional</p>
          <p>Use as Regras para a área principal; adicione aqui os locais extras.</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Locais de referência do órgão</caption>
            <thead>
              <tr>
                <th scope="col">Local</th>
                <th scope="col">Coordenadas</th>
                <th scope="col">Raio</th>
                <th scope="col">Ação</th>
              </tr>
            </thead>
            <tbody>
              {locais.map((l) => (
                <tr key={l.id}>
                  <td>{l.nome}</td>
                  <td style={{ fontSize: 13 }}>
                    <a href={`https://www.google.com/maps?q=${l.latitude},${l.longitude}`}
                       target="_blank" rel="noopener noreferrer">
                      {l.latitude.toFixed(5)}, {l.longitude.toFixed(5)}
                    </a>
                  </td>
                  <td>{l.raioMetros} m</td>
                  <td>
                    <button className="br-button br-button--sm br-button--danger"
                            onClick={() => void remover(l.id)}
                            aria-label={`Remover o local ${l.nome}`}>
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  )
}

// ---- Criar Lotação ----
function CriarLotacaoModal({ onClose, onSaved }: {
  onClose: () => void
  onSaved: (msg: string) => void
}) {
  const [nome, setNome] = useState('')
  const [sigla, setSigla] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body: CriarLotacaoRequest = { nome: nome.trim(), sigla: sigla.trim() || undefined }
      await api.post<LotacaoResponse>('/api/lotacoes', body)
      onSaved('Lotação criada com sucesso.')
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title="Nova Lotação"
      onClose={onClose}
      footer={
        <>
          <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
          <button
            className="br-button br-button--primary"
            form="form-criar-lotacao"
            type="submit"
            disabled={saving}
          >
            {saving ? 'Salvando…' : 'Criar'}
          </button>
        </>
      }
    >
      {error != null && <ErrorAlert error={error} />}
      <form id="form-criar-lotacao" onSubmit={handleSubmit} noValidate>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="lotacao-nome">Nome</label>
          <input
            id="lotacao-nome"
            className="br-input"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            required
            autoFocus
          />
        </div>
        <div className="br-form-group">
          <label className="br-label" htmlFor="lotacao-sigla">Sigla</label>
          <input
            id="lotacao-sigla"
            className="br-input"
            value={sigla}
            onChange={(e) => setSigla(e.target.value)}
            placeholder="ex: RH, SEOP"
          />
        </div>
      </form>
    </Modal>
  )
}

// ---- Regras de Ponto ----
function RegrasModal({ lotacao, jornadas, onClose, onSaved }: {
  lotacao: LotacaoResponse
  jornadas: JornadaResponse[]
  onClose: () => void
  onSaved: (msg: string) => void
}) {
  const [jornadaId, setJornadaId] = useState(lotacao.jornadaPadraoId ?? '')
  const [tolerancia, setTolerancia] = useState(String(lotacao.toleranciaMinutos ?? 0))
  const [bancoHoras, setBancoHoras] = useState(lotacao.bancoHorasHabilitado ?? false)
  const [tetoBancoHoras, setTetoBancoHoras] = useState(String(lotacao.tetoBancoHorasMinutos ?? ''))
  const [verificacao, setVerificacao] = useState(lotacao.verificacaoObrigatoria ?? false)
  const [teletrabalho, setTeletrabalho] = useState(lotacao.teletrabalho ?? false)
  const [adaptacaoAte, setAdaptacaoAte] = useState(lotacao.adaptacaoAte ?? '')
  const [lat, setLat] = useState(String(lotacao.geofenceLatitude ?? ''))
  const [lon, setLon] = useState(String(lotacao.geofenceLongitude ?? ''))
  const [raio, setRaio] = useState(String(lotacao.geofenceRaioMetros ?? ''))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body: DefinirRegrasRequest = {
        jornadaPadraoId: jornadaId || undefined,
        toleranciaMinutos: tolerancia ? Number(tolerancia) : undefined,
        bancoHorasHabilitado: bancoHoras,
        tetoBancoHorasMinutos: tetoBancoHoras ? Number(tetoBancoHoras) : null,
        verificacaoObrigatoria: verificacao,
        adaptacaoAte: adaptacaoAte || null,
        teletrabalho: teletrabalho,
        geofenceLatitude: lat ? Number(lat) : null,
        geofenceLongitude: lon ? Number(lon) : null,
        geofenceRaioMetros: raio ? Number(raio) : undefined,
      }
      await api.put<LotacaoResponse>(`/api/lotacoes/${lotacao.id}/regras`, body)
      onSaved(`Regras de "${lotacao.nome}" atualizadas.`)
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={`Regras de Ponto — ${lotacao.nome}`}
      onClose={onClose}
      footer={
        <>
          <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
          <button
            className="br-button br-button--primary"
            form="form-regras"
            type="submit"
            disabled={saving}
          >
            {saving ? 'Salvando…' : 'Salvar'}
          </button>
        </>
      }
    >
      {error != null && <ErrorAlert error={error} />}
      <form id="form-regras" onSubmit={handleSubmit} noValidate>
        <div className="br-form-group">
          <label className="br-label" htmlFor="jornada-padrao">Jornada Padrão</label>
          <select
            id="jornada-padrao"
            className="br-select"
            value={jornadaId}
            onChange={(e) => setJornadaId(e.target.value)}
          >
            <option value="">— Nenhuma —</option>
            {jornadas.map((j) => (
              <option key={j.id} value={j.id}>{j.nome}</option>
            ))}
          </select>
        </div>

        <div className="br-form-group">
          <label className="br-label" htmlFor="tolerancia">Tolerância (minutos)</label>
          <input
            id="tolerancia"
            type="number"
            min="0"
            className="br-input"
            value={tolerancia}
            onChange={(e) => setTolerancia(e.target.value)}
          />
        </div>

        <div className="br-form-group">
          <label className="br-toggle">
            <input
              type="checkbox"
              checked={bancoHoras}
              onChange={(e) => setBancoHoras(e.target.checked)}
              aria-label="Banco de horas habilitado"
            />
            <span>Banco de horas habilitado</span>
          </label>
        </div>

        <div className="br-form-group">
          <label className="br-label" htmlFor="teto-bh">Teto do banco de horas (minutos)</label>
          <input
            id="teto-bh"
            type="number"
            min="0"
            className="br-input"
            value={tetoBancoHoras}
            onChange={(e) => setTetoBancoHoras(e.target.value)}
            placeholder="padrão do sistema: 12000 (200h)"
            disabled={!bancoHoras}
          />
        </div>

        <div className="br-form-group">
          <label className="br-toggle">
            <input
              type="checkbox"
              checked={verificacao}
              onChange={(e) => setVerificacao(e.target.checked)}
              aria-label="Exigir verificação na batida"
            />
            <span>Exigir verificação na batida (biometria/PIN/desenho do aparelho, ou facial)</span>
          </label>
        </div>

        <div className="br-form-group">
          <label className="br-label" htmlFor="adaptacao-ate">Modo adaptação até</label>
          <input
            id="adaptacao-ate"
            type="date"
            className="br-input"
            value={adaptacaoAte}
            onChange={(e) => setAdaptacaoAte(e.target.value)}
          />
          <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '4px 0 0' }}>
            Até esta data o órgão só registra: atrasos, faltas e saídas antecipadas não descontam nem
            penalizam (período de adaptação). Deixe em branco para apurar normalmente.
          </p>
        </div>

        <fieldset style={{ border: '1px solid var(--color-gray-8)', borderRadius: 4, padding: '12px 16px', marginTop: 8 }}>
          <legend style={{ fontSize: 13, fontWeight: 600, padding: '0 4px' }}>Localização (opcional)</legend>
          <p style={{ fontSize: 12, color: 'var(--color-gray-60)', margin: '0 0 10px' }}>
            Define a área de referência do órgão. <strong>Não bloqueia nem avisa o servidor</strong>:
            serve apenas para o administrador conferir, no registro de ponto, onde a batida foi feita
            (batidas fora da área ficam sinalizadas). Deixe em branco se o órgão não precisa.
          </p>
          <div className="form-row">
            <div className="br-form-group">
              <label className="br-label" htmlFor="geo-lat">Latitude</label>
              <input
                id="geo-lat"
                type="number"
                step="any"
                className="br-input"
                value={lat}
                onChange={(e) => setLat(e.target.value)}
                placeholder="-23.5505"
              />
            </div>
            <div className="br-form-group">
              <label className="br-label" htmlFor="geo-lon">Longitude</label>
              <input
                id="geo-lon"
                type="number"
                step="any"
                className="br-input"
                value={lon}
                onChange={(e) => setLon(e.target.value)}
                placeholder="-46.6333"
              />
            </div>
          </div>
          <div className="br-form-group">
            <label className="br-label" htmlFor="geo-raio">Raio (metros)</label>
            <input
              id="geo-raio"
              type="number"
              min="1"
              className="br-input"
              value={raio}
              onChange={(e) => setRaio(e.target.value)}
              placeholder="100"
            />
          </div>
          <div className="br-form-group">
            <label className="br-toggle">
              <input
                type="checkbox"
                checked={teletrabalho}
                onChange={(e) => setTeletrabalho(e.target.checked)}
                aria-label="Teletrabalho / home office"
              />
              <span>Teletrabalho / home office (a geofence não se aplica — bate de qualquer lugar)</span>
            </label>
          </div>
        </fieldset>
      </form>
    </Modal>
  )
}

// ---- Definir Chefia ----
function ChefiaModal({ lotacao, servidores, onClose, onSaved }: {
  lotacao: LotacaoResponse
  servidores: ServidorResponse[]
  onClose: () => void
  onSaved: (msg: string) => void
}) {
  const [servidorId, setServidorId] = useState(lotacao.chefiaServidorId ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!servidorId) { setError(new Error('Selecione um servidor.')); return }
    setSaving(true)
    setError(null)
    try {
      await api.put<unknown>(`/api/lotacoes/${lotacao.id}/chefia?servidorId=${servidorId}`)
      onSaved(`Chefia de "${lotacao.nome}" definida.`)
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={`Definir Chefia — ${lotacao.nome}`}
      onClose={onClose}
      footer={
        <>
          <button className="br-button br-button--secondary" onClick={onClose}>Cancelar</button>
          <button
            className="br-button br-button--primary"
            form="form-chefia"
            type="submit"
            disabled={saving}
          >
            {saving ? 'Salvando…' : 'Definir'}
          </button>
        </>
      }
    >
      {error != null && <ErrorAlert error={error} />}
      <form id="form-chefia" onSubmit={handleSubmit} noValidate>
        <div className="br-form-group">
          <label className="br-label br-label--required" htmlFor="chefia-servidor">Servidor</label>
          <select
            id="chefia-servidor"
            className="br-select"
            value={servidorId}
            onChange={(e) => setServidorId(e.target.value)}
            required
          >
            <option value="">— Selecione —</option>
            {servidores.map((s) => (
              <option key={s.id} value={s.id}>{s.nome} — {s.cpf}</option>
            ))}
          </select>
        </div>
      </form>
    </Modal>
  )
}
