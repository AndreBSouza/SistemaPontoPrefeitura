import { describe, it, expect } from 'vitest'
import { decodeJwt, papeisDoToken, tenantDoToken } from './jwt'

function fakeJwt(payload: object): string {
  const b64 = btoa(JSON.stringify(payload))
  return `cabecalho.${b64}.assinatura`
}

describe('jwt', () => {
  it('decodifica o payload', () => {
    const t = fakeJwt({ sub: '123', tenant_id: 'ente-a' })
    expect(decodeJwt(t)).toMatchObject({ sub: '123', tenant_id: 'ente-a' })
  })

  it('token malformado retorna null', () => {
    expect(decodeJwt('nao-e-um-jwt')).toBeNull()
  })

  it('extrai só os papéis conhecidos de realm_access.roles', () => {
    const t = fakeJwt({ realm_access: { roles: ['rh', 'gestor', 'offline_access', 'desconhecido'] } })
    expect(papeisDoToken(t)).toEqual(['rh', 'gestor'])
  })

  it('sem realm_access -> sem papéis', () => {
    expect(papeisDoToken(fakeJwt({ sub: '1' }))).toEqual([])
  })

  it('extrai tenant_id e nome', () => {
    const t = fakeJwt({ tenant_id: 'ente-b', tenant_name: 'Prefeitura B' })
    expect(tenantDoToken(t)).toEqual({ id: 'ente-b', nome: 'Prefeitura B' })
  })

  it('sem tenant_id -> null', () => {
    expect(tenantDoToken(fakeJwt({ sub: '1' }))).toBeNull()
  })
})
