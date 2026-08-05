/**
 * Integração OIDC (Keycloak) do painel — Authorization Code + PKCE via oidc-client-ts.
 *
 * Fica ATRÁS de env: só ativa se VITE_OIDC_AUTHORITY e VITE_OIDC_CLIENT_ID estiverem definidos.
 * Sem eles, o app usa o login dev (Tenant ID) — nada muda. Ao logar, popula os MESMOS storages
 * do login dev (pm_tenant_id / pm_roles / pm_tenant_name) + o pm_access_token (Bearer), então o
 * resto do app — TenantContext, filtro de menu por papel, api/client — funciona sem alteração.
 *
 * Config Keycloak: crie um client público 'ponto-web' (Standard Flow + PKCE S256), redirect
 * <origin>/callback; mapeie o claim tenant_id no token; os papéis vêm de realm_access.roles.
 */
import { UserManager, WebStorageStateStore } from 'oidc-client-ts'
import type { Papel } from './roles'
import { papeisDoToken, tenantDoToken, TOKEN_KEY } from './jwt'
import { CHAVE_PAPEIS, CHAVE_TENANT, CHAVE_TENANT_NOME, limparSessao } from './sessao'

export { TOKEN_KEY }

const env = import.meta.env
const AUTHORITY = env.VITE_OIDC_AUTHORITY as string | undefined
const CLIENT_ID = env.VITE_OIDC_CLIENT_ID as string | undefined
const REDIRECT = (env.VITE_OIDC_REDIRECT as string | undefined) ?? `${window.location.origin}/callback`
const POST_LOGOUT = (env.VITE_OIDC_POST_LOGOUT as string | undefined) ?? window.location.origin

export function oidcAtivo(): boolean {
  return Boolean(AUTHORITY && CLIENT_ID)
}

let mgr: UserManager | null = null
function manager(): UserManager {
  if (!mgr) {
    mgr = new UserManager({
      authority: AUTHORITY as string,
      client_id: CLIENT_ID as string,
      redirect_uri: REDIRECT,
      post_logout_redirect_uri: POST_LOGOUT,
      response_type: 'code',
      scope: 'openid profile',
      userStore: new WebStorageStateStore({ store: window.localStorage }),
      // Renova o access token antes de expirar (usa o refresh token do Keycloak). Sem isso, o
      // token do IdP vence em minutos e TODAS as chamadas do painel passam a dar 401.
      automaticSilentRenew: true,
      accessTokenExpiringNotificationTimeInSeconds: 60,
    })
  }
  return mgr
}

/** Grava a sessão local (token + ente + papéis) a partir do access token. */
function gravarSessao(token: string): boolean {
  const tenant = tenantDoToken(token)
  if (!tenant) return false
  const roles: Papel[] = papeisDoToken(token)
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(CHAVE_TENANT, tenant.id)
  if (tenant.nome) localStorage.setItem(CHAVE_TENANT_NOME, tenant.nome)
  localStorage.setItem(CHAVE_PAPEIS, JSON.stringify(roles.length ? roles : ['rh']))
  return true
}

/**
 * Liga a renovação automática da sessão. Chamado no boot do app: depois do redirect para
 * /dashboard o UserManager em memória se perde, então é preciso reinstanciá-lo para que o timer
 * de renovação exista e o token renovado seja regravado no storage que o api/client lê.
 */
export function iniciarSessaoOidc(): void {
  if (!oidcAtivo() || !localStorage.getItem(TOKEN_KEY)) return
  const m = manager()
  m.events.addUserLoaded((user) => { gravarSessao(user.access_token) })
  // Renovação impossível (refresh token expirado/revogado): encerra e manda para o login.
  m.events.addSilentRenewError(() => encerrarSessaoExpirada())
  void m.getUser().catch(() => undefined) // reidrata o usuário do storage e arma o timer
}

/**
 * Tenta renovar a sessão silenciosamente (refresh token). Usado como última chance antes de
 * deslogar quando uma chamada volta 401 — evita expulsar o usuário por uma janela de renovação
 * perdida (aba em segundo plano, máquina que hibernou).
 */
export async function renovarSessaoOidc(): Promise<boolean> {
  if (!oidcAtivo()) return false
  try {
    const user = await manager().signinSilent()
    return user ? gravarSessao(user.access_token) : false
  } catch {
    return false
  }
}

/** Encerra a sessão expirada e leva ao login (sem loop se já estiver lá). */
export function encerrarSessaoExpirada(): void {
  limparSessao()
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login?expirado=1'
  }
}

/** Redireciona para o Keycloak. */
export async function loginOidc(): Promise<void> {
  await manager().signinRedirect()
}

/** Processa o retorno (/callback): popula a sessão local a partir do token. */
export async function completarLoginOidc(): Promise<boolean> {
  const user = await manager().signinRedirectCallback()
  return gravarSessao(user.access_token)
}

/**
 * Logout no Keycloak + encerra a sessão local.
 *
 * Limpa TODA a sessão (não só o token): o post-logout volta para a raiz, e se pm_tenant_id/pm_roles
 * sobrevivessem, o guard de rota deixaria entrar de novo com os papéis antigos — e, sem token, o
 * client cairia no X-Tenant-Id (modo dev). Em PC compartilhado de prefeitura, isso é "sair sem sair".
 */
export async function logoutOidc(): Promise<void> {
  limparSessao()
  try {
    await manager().removeUser()
  } catch {
    // Sessão local já limpa; segue para o logout no IdP.
  }
  try {
    await manager().signoutRedirect()
  } catch {
    window.location.href = '/login'
  }
}
