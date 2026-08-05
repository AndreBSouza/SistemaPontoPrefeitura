import { useEffect, useState } from 'react'
import { Modal } from '../../components/Modal'
import { Alert, ErrorAlert } from '../../components/Alert'
import { Loading } from '../../components/Loading'
import { api } from '../../api/client'
import type { RegistroComprovante } from '../../api/types'

export function RegistrosLocalizacaoModal({ vinculoId, onClose }: {
  vinculoId: string
  onClose: () => void
}) {
  const [registros, setRegistros] = useState<RegistroComprovante[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<unknown>(null)

  useEffect(() => {
    setLoading(true)
    api
      .get<RegistroComprovante[]>(`/api/registros?vinculoId=${vinculoId}`)
      .then((r) => setRegistros([...r].reverse())) // mais recentes primeiro
      .catch(setError)
      .finally(() => setLoading(false))
  }, [vinculoId])

  return (
    <Modal title="Registros e localização" onClose={onClose}>
      {error != null && <ErrorAlert error={error} />}
      <Alert variant="info">
        A localização serve para o administrador conferir onde cada batida foi feita. O servidor não é
        bloqueado nem avisado; batidas fora da área de referência do órgão ficam sinalizadas.
      </Alert>
      {loading ? (
        <Loading label="Carregando registros…" />
      ) : registros.length === 0 ? (
        <div className="empty-state">
          <p className="empty-state__title">Nenhum registro</p>
          <p>Este vínculo ainda não possui batidas de ponto.</p>
        </div>
      ) : (
        <div className="br-table-wrapper">
          <table className="br-table">
            <caption className="visually-hidden">Registros de ponto e localização</caption>
            <thead>
              <tr>
                <th scope="col">Data/hora</th>
                <th scope="col">Tipo</th>
                <th scope="col">Origem</th>
                <th scope="col">Localização</th>
              </tr>
            </thead>
            <tbody>
              {registros.map((r) => (
                <tr key={r.id}>
                  <td style={{ fontSize: 13 }}>{new Date(r.dataHoraServidor).toLocaleString('pt-BR')}</td>
                  <td>{r.tipo}</td>
                  <td style={{ fontSize: 13 }}>{r.origem}</td>
                  <td style={{ fontSize: 13 }}>
                    {r.latitude != null && r.longitude != null ? (
                      <span style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                        <a
                          href={`https://www.google.com/maps?q=${r.latitude},${r.longitude}`}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          {r.latitude.toFixed(5)}, {r.longitude.toFixed(5)} — ver no mapa
                        </a>
                        {r.foraDaCerca && <span className="br-badge br-badge--warning">fora da área</span>}
                      </span>
                    ) : (
                      <em style={{ color: 'var(--color-gray-40)' }}>sem localização</em>
                    )}
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
