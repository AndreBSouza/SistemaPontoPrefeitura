/**
 * Chaves da sessão local e limpeza centralizada.
 *
 * Existe um único lugar que sabe QUAIS chaves formam a sessão — logout, expiração de token e
 * troca de modo (OIDC ↔ dev) usam todos a mesma limpeza. Quando isso ficava espalhado, o logout
 * apagava só o token e o painel reabria "logado" com os papéis antigos.
 */
import { TOKEN_KEY } from './jwt'

export const CHAVE_TENANT = 'pm_tenant_id'
export const CHAVE_TENANT_NOME = 'pm_tenant_name'
export const CHAVE_PAPEIS = 'pm_roles'

/** Remove TODA a sessão do painel (token + ente + papéis). */
export function limparSessao(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(CHAVE_TENANT)
  localStorage.removeItem(CHAVE_TENANT_NOME)
  localStorage.removeItem(CHAVE_PAPEIS)
}
