/**
 * API Client — wrapper tipado para fetch → /api/...
 *
 * - PRODUÇÃO (OIDC): quando há pm_access_token (login Keycloak), envia Authorization: Bearer.
 *   O backend extrai tenant + papéis do próprio token — nada mais é necessário.
 * - DEV: sem token, envia X-Tenant-Id do localStorage (login por Tenant ID).
 * - Centraliza tratamento de erros do ApiExceptionHandler do backend
 */
import { TOKEN_KEY } from '../auth/jwt'
import { encerrarSessaoExpirada, renovarSessaoOidc } from '../auth/oidc'

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/**
 * Retorna os headers de identificação para as requisições.
 * - OIDC (Keycloak): Authorization: Bearer <token>. O token é autoritativo (traz tenant + papéis),
 *   então NÃO se envia X-Tenant-Id junto.
 * - DEV: X-Tenant-Id do localStorage.
 */
function getTenantHeader(path: string): Record<string, string> {
  const headers: Record<string, string> = {}
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  } else {
    const tenantId = localStorage.getItem('pm_tenant_id')
    if (tenantId) headers['X-Tenant-Id'] = tenantId
  }
  // O token de dispositivo autentica APENAS o app do servidor (/api/me/*). Nunca vai nas
  // chamadas do painel admin — senão um token obsoleto no navegador derruba TODAS com 401.
  const deviceToken = localStorage.getItem('pm_device_token')
  if (deviceToken && path.startsWith('/api/me')) headers['X-Device-Token'] = deviceToken
  return headers
}

async function parseError(resp: Response): Promise<ApiError> {
  try {
    const body = await resp.json() as { message?: string; code?: string }
    const msg = body.message ?? `HTTP ${resp.status}`
    const code = body.code ?? String(resp.status)
    return new ApiError(resp.status, code, msg)
  } catch {
    return new ApiError(resp.status, String(resp.status), `HTTP ${resp.status}`)
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  jaRenovou = false
): Promise<T> {
  const tenantHeaders = getTenantHeader(path)
  const isFormData = options.body instanceof FormData

  const headers: Record<string, string> = {
    ...tenantHeaders,
    ...(options.headers as Record<string, string> | undefined),
  }
  // FormData define o próprio boundary; respeita um Content-Type explícito (ex.: text/csv);
  // caso contrário, assume JSON.
  if (!isFormData && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }

  const resp = await fetch(path, { ...options, headers })

  if (!resp.ok) {
    if (resp.status === 401 && localStorage.getItem(TOKEN_KEY) && !jaRenovou) {
      // Última chance antes de expulsar o usuário: renova em silêncio (aba em segundo plano ou
      // máquina que hibernou perdem a janela de renovação automática) e repete a chamada UMA vez.
      if (await renovarSessaoOidc()) {
        return apiFetch<T>(path, options, true)
      }
      // Sem renovação possível: encerra e volta ao login. Sem isto o painel fica "morto"
      // (toda chamada em 401, cada página com seu erro genérico e nenhuma saída para o usuário).
      encerrarSessaoExpirada()
    }
    throw await parseError(resp)
  }

  // 204 No Content
  if (resp.status === 204) return undefined as T

  const contentType = resp.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) {
    return resp.json() as Promise<T>
  }
  // CSV / plain text
  return resp.text() as unknown as T
}

export const api = {
  get: <T>(path: string) => apiFetch<T>(path),
  post: <T>(path: string, body?: unknown) =>
    apiFetch<T>(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    apiFetch<T>(path, { method: 'PUT', body: body !== undefined ? JSON.stringify(body) : undefined }),
  postForm: <T>(path: string, form: FormData) =>
    apiFetch<T>(path, { method: 'POST', body: form }),
  /** POST de texto puro (ex.: CSV) — envia Content-Type text/csv (não JSON). */
  postText: <T>(path: string, text: string) =>
    apiFetch<T>(path, { method: 'POST', body: text, headers: { 'Content-Type': 'text/csv' } }),
  /** POST de bytes (ex.: imagem) — envia o Content-Type informado (não JSON). */
  postBinary: <T>(path: string, data: Blob | ArrayBuffer, contentType: string) =>
    apiFetch<T>(path, { method: 'POST', body: data, headers: { 'Content-Type': contentType } }),
  delete: <T>(path: string) => apiFetch<T>(path, { method: 'DELETE' }),
}
