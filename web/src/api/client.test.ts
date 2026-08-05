import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { api, ApiError } from './client'

// localStorage in-memory (independente do ambiente) + fetch falso (jsdom não
// implementa a Fetch API). Cobre o tratamento de erro e o header de tenant.
const store: Record<string, string> = {}
const localStorageStub = {
  getItem: (k: string): string | null => (k in store ? store[k] : null),
  setItem: (k: string, v: string) => {
    store[k] = v
  },
  removeItem: (k: string) => {
    delete store[k]
  },
  clear: () => {
    for (const k of Object.keys(store)) delete store[k]
  },
}

function mockFetch(status: number, body: unknown, contentType = 'application/json') {
  const resp = {
    ok: status >= 200 && status < 300,
    status,
    headers: {
      get: (h: string) => (h.toLowerCase() === 'content-type' ? contentType : null),
    },
    json: async () => body,
    text: async () => (typeof body === 'string' ? body : JSON.stringify(body)),
  }
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(resp))
}

beforeEach(() => {
  localStorageStub.clear()
  vi.stubGlobal('localStorage', localStorageStub)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('apiFetch', () => {
  it('retorna JSON em caso de sucesso', async () => {
    mockFetch(200, { valor: 42 })
    const r = await api.get<{ valor: number }>('/api/x')
    expect(r.valor).toBe(42)
  })

  it('lança ApiError em caso de erro do backend', async () => {
    mockFetch(409, { message: 'conflito', code: 'CONFLITO' })
    await expect(api.get('/api/x')).rejects.toBeInstanceOf(ApiError)
  })

  it('envia o header X-Tenant-Id quando há ente salvo', async () => {
    localStorage.setItem('pm_tenant_id', 'ente-x')
    mockFetch(200, { ok: true })
    await api.get('/api/x')
    const chamada = (fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls[0]
    const headers = (chamada[1] as RequestInit).headers as Record<string, string>
    expect(headers['X-Tenant-Id']).toBe('ente-x')
  })

  it('em OIDC envia Bearer e NÃO envia X-Tenant-Id (o token é autoritativo)', async () => {
    localStorage.setItem('pm_tenant_id', 'ente-x')
    localStorage.setItem('pm_access_token', 'tok-123')
    mockFetch(200, { ok: true })
    await api.get('/api/x')
    const chamada = (fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls[0]
    const headers = (chamada[1] as RequestInit).headers as Record<string, string>
    expect(headers['Authorization']).toBe('Bearer tok-123')
    expect(headers['X-Tenant-Id']).toBeUndefined()
  })
})
