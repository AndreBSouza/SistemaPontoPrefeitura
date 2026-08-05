import { useEffect, useState } from 'react'
import { Modal } from '../../components/Modal'
import { Alert, ErrorAlert } from '../../components/Alert'
import { Loading } from '../../components/Loading'
import { api } from '../../api/client'
import { formatarDataHora } from '../../utils/formato'
import type { GerarCodigoResponse } from '../../api/types'

export function GerarCodigoModal({ vinculoId, onClose }: {
  vinculoId: string
  onClose: () => void
}) {
  const [codigo, setCodigo] = useState<GerarCodigoResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [copiado, setCopiado] = useState(false)
  const [falhaCopia, setFalhaCopia] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function gerar() {
    setLoading(true)
    setError(null)
    try {
      const res = await api.post<GerarCodigoResponse>('/api/ativacao/codigos', { vinculoId })
      setCodigo(res)
    } catch (err) {
      setError(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void gerar() }, [])

  async function copiar() {
    if (!codigo) return
    // navigator.clipboard não existe fora de contexto seguro (painel em http na intranet do
    // município). Sem o fallback, o clique não fazia nada e o admin podia achar que copiou.
    try {
      if (!navigator.clipboard) throw new Error('sem clipboard')
      await navigator.clipboard.writeText(codigo.codigo)
      setCopiado(true)
      setTimeout(() => setCopiado(false), 2500)
    } catch {
      // Seleciona o código na tela para o usuário copiar com Ctrl+C.
      const el = document.getElementById('codigo-ativacao')
      if (el) {
        const faixa = document.createRange()
        faixa.selectNodeContents(el)
        const selecao = window.getSelection()
        selecao?.removeAllRanges()
        selecao?.addRange(faixa)
      }
      setFalhaCopia(true)
    }
  }

  return (
    <Modal
      title="Código de Ativação do Aplicativo"
      onClose={onClose}
      footer={
        <button className="br-button br-button--secondary" onClick={onClose}>Fechar</button>
      }
    >
      {error != null && <ErrorAlert error={error} />}

      {loading && <Loading label="Gerando código…" />}

      {codigo && !loading && (
        <>
          <Alert variant="warning">
            <strong>Atenção:</strong> este código é mostrado <strong>uma única vez</strong>.
            Anote ou copie antes de fechar.
          </Alert>

          <div style={{ textAlign: 'center', margin: '24px 0 16px' }}>
            <p style={{ fontSize: 13, color: 'var(--color-gray-60)', marginBottom: 8 }}>
              Código de ativação
            </p>
            <div style={{
              fontFamily: 'monospace',
              fontSize: 42,
              fontWeight: 700,
              letterSpacing: 8,
              color: 'var(--color-primary)',
              background: 'var(--color-gray-2)',
              border: '2px dashed var(--color-gray-20)',
              borderRadius: 8,
              padding: '16px 24px',
              display: 'inline-block',
              userSelect: 'all',
            }}
              id="codigo-ativacao"
              aria-label={`Código de ativação: ${codigo.codigo}`}
            >
              {codigo.codigo}
            </div>
          </div>

          <div style={{ textAlign: 'center', marginBottom: 16 }}>
            <button
              className="br-button br-button--primary"
              onClick={() => void copiar()}
              aria-label="Copiar código de ativação"
            >
              {copiado ? '✅ Copiado!' : '📋 Copiar código'}
            </button>
            {falhaCopia && (
              <p role="status" style={{ fontSize: 13, color: 'var(--color-gray-60)', marginTop: 8 }}>
                O navegador bloqueou a cópia automática. O código já está selecionado — use Ctrl+C.
              </p>
            )}
          </div>

          <p style={{ textAlign: 'center', fontSize: 13, color: 'var(--color-gray-60)' }}>
            Válido até: <strong>{formatarDataHora(codigo.expiraEm)}</strong>
          </p>

          <div style={{ marginTop: 16 }}>
            <Alert variant="info">
              Dite este código ao servidor para que ele o informe no aplicativo móvel.
              O código expira automaticamente após o prazo acima.
            </Alert>
          </div>
        </>
      )}

      {/* Também aparece após erro: antes o admin precisava fechar e reabrir o modal p/ tentar de novo. */}
      {!loading && !codigo && (
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <button className="br-button br-button--primary" onClick={() => void gerar()}>
            {error != null ? 'Tentar novamente' : 'Gerar Código'}
          </button>
        </div>
      )}
    </Modal>
  )
}
