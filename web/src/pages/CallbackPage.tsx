/**
 * Retorno do Keycloak (redirect_uri = /callback). Troca o code por token, popula a sessão local
 * e recarrega em /dashboard (reload garante que o TenantProvider releia o storage já populado).
 */
import { useEffect, useState } from 'react'
import { completarLoginOidc } from '../auth/oidc'
import { TOKEN_KEY } from '../auth/jwt'

/**
 * Guard fora do ciclo do React: o `code` e o `state` do OIDC são de uso único, então a segunda
 * execução do efeito (StrictMode em dev) falharia com erro técnico mesmo tendo dado tudo certo.
 */
let processamento: Promise<boolean> | null = null

export default function CallbackPage() {
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    let vivo = true
    processamento ??= completarLoginOidc()
    processamento
      .then((ok) => {
        if (!vivo) return
        if (ok) {
          window.location.href = '/dashboard'
        } else {
          // Autenticou no IdP, mas o token não traz o ente: sem isto o usuário voltaria ao login
          // sem explicação e tentaria de novo em loop.
          setErro('Login autenticado, mas o token não contém o ente (claim tenant_id). '
            + 'Peça ao administrador para configurar o mapper tenant_id no client do Keycloak.')
        }
      })
      .catch((e: unknown) => {
        if (!vivo) return
        // Recarregar /callback reprocessa um code já consumido; se a sessão foi gravada antes,
        // seguir para o painel em vez de mostrar erro.
        if (localStorage.getItem(TOKEN_KEY)) {
          window.location.href = '/dashboard'
          return
        }
        processamento = null // permite nova tentativa
        setErro(e instanceof Error ? e.message : String(e))
      })
    return () => { vivo = false }
  }, [])

  return (
    <div className="login-page">
      <div className="login-card" role="status" aria-live="polite">
        {erro ? (
          <>
            <p style={{ color: 'var(--color-danger)', fontWeight: 600 }}>Falha ao concluir o login.</p>
            <p style={{ fontSize: 14 }}>{erro}</p>
            <a className="br-button br-button--secondary br-button--block" href="/login">Voltar ao login</a>
          </>
        ) : (
          <p>Concluindo o login…</p>
        )}
      </div>
    </div>
  )
}
