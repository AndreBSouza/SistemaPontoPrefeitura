/**
 * TenantContext — tenant ativo (X-Tenant-Id) + papéis do usuário.
 * Em dev: tenant e papéis são escolhidos na tela /login e persistidos no localStorage.
 * Em produção: virão do bearer token Keycloak (claim tenant_id + realm_access.roles).
 */
import { createContext, useContext, useState, ReactNode } from 'react'
import type { Papel } from '../auth/roles'
import { TOKEN_KEY } from '../auth/jwt'

interface TenantContextValue {
  tenantId: string | null
  tenantName: string | null
  roles: Papel[]
  setTenant: (id: string, name?: string, roles?: Papel[]) => void
  clearTenant: () => void
}

const TenantContext = createContext<TenantContextValue | null>(null)

const STORAGE_KEY = 'pm_tenant_id'
const STORAGE_NAME_KEY = 'pm_tenant_name'
const STORAGE_ROLES_KEY = 'pm_roles'

function lerPapeis(): Papel[] {
  try {
    const raw = localStorage.getItem(STORAGE_ROLES_KEY)
    if (!raw) return ['tenant-admin'] // default seguro p/ sessões antigas: admin do ente
    const arr = JSON.parse(raw) as Papel[]
    return Array.isArray(arr) && arr.length > 0 ? arr : ['tenant-admin']
  } catch {
    return ['tenant-admin']
  }
}

export function TenantProvider({ children }: { children: ReactNode }) {
  const [tenantId, setTenantId] = useState<string | null>(() => localStorage.getItem(STORAGE_KEY))
  const [tenantName, setTenantName] = useState<string | null>(() => localStorage.getItem(STORAGE_NAME_KEY))
  const [roles, setRoles] = useState<Papel[]>(() => lerPapeis())

  function setTenant(id: string, name?: string, papeis?: Papel[]) {
    // Login dev explícito invalida qualquer sessão OIDC anterior: o api/client dá precedência ao
    // Bearer, então um token vencido esquecido no storage derrubaria todas as chamadas com 401.
    localStorage.removeItem(TOKEN_KEY)
    localStorage.setItem(STORAGE_KEY, id)
    if (name) localStorage.setItem(STORAGE_NAME_KEY, name)
    else localStorage.removeItem(STORAGE_NAME_KEY)
    const p = papeis && papeis.length > 0 ? papeis : (['tenant-admin'] as Papel[])
    localStorage.setItem(STORAGE_ROLES_KEY, JSON.stringify(p))
    setTenantId(id)
    setTenantName(name ?? null)
    setRoles(p)
  }

  function clearTenant() {
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(STORAGE_NAME_KEY)
    localStorage.removeItem(STORAGE_ROLES_KEY)
    localStorage.removeItem(TOKEN_KEY)
    setTenantId(null)
    setTenantName(null)
    setRoles(['tenant-admin'])
  }

  return (
    <TenantContext.Provider value={{ tenantId, tenantName, roles, setTenant, clearTenant }}>
      {children}
    </TenantContext.Provider>
  )
}

export function useTenant(): TenantContextValue {
  const ctx = useContext(TenantContext)
  if (!ctx) throw new Error('useTenant must be used inside TenantProvider')
  return ctx
}
