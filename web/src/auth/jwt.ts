/**
 * Leitura (sem verificação de assinatura) dos claims de um JWT no cliente — só para popular a UI
 * (papéis do menu + nome do ente). A verificação real é do backend (resource server Keycloak).
 */
import type { Papel } from './roles'

/** Chave do access_token OIDC no localStorage. Presença = sessão Keycloak ativa (envia Bearer). */
export const TOKEN_KEY = 'pm_access_token'

const PAPEIS_VALIDOS: readonly string[] = ['rh', 'gestor', 'controladoria', 'tenant-admin', 'operador']

function base64UrlParaTexto(b64url: string): string {
  const b64 = b64url.replace(/-/g, '+').replace(/_/g, '/')
  const bin = atob(b64)
  const bytes = Uint8Array.from(bin, (c) => c.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

/** Decodifica o payload de um JWT. Retorna null se malformado. */
export function decodeJwt(token: string): Record<string, unknown> | null {
  try {
    const parte = token.split('.')[1]
    if (!parte) return null
    return JSON.parse(base64UrlParaTexto(parte)) as Record<string, unknown>
  } catch {
    return null
  }
}

/** Papéis do token: realm_access.roles ∩ papéis conhecidos do sistema. */
export function papeisDoToken(token: string): Papel[] {
  const claims = decodeJwt(token)
  const realm = claims?.['realm_access'] as { roles?: unknown } | undefined
  const roles = Array.isArray(realm?.roles) ? (realm!.roles as unknown[]) : []
  return roles.map(String).filter((r) => PAPEIS_VALIDOS.includes(r)) as Papel[]
}

/** tenant_id + nome (claim tenant_name, senão name) do token. */
export function tenantDoToken(token: string): { id: string; nome: string | null } | null {
  const claims = decodeJwt(token)
  const id = claims?.['tenant_id']
  if (typeof id !== 'string' || !id) return null
  const nome = (claims?.['tenant_name'] ?? claims?.['name'])
  return { id, nome: typeof nome === 'string' ? nome : null }
}
