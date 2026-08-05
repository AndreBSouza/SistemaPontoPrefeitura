/**
 * Tela de login.
 *
 * PRODUÇÃO (VITE_OIDC_* definidas): botão "Entrar com Keycloak" → redirect OIDC. O tenant_id e os
 * papéis vêm como claims no JWT Bearer. Sem gov.br.
 * DEV (sem as envs): operador informa o X-Tenant-Id + papéis manualmente.
 */
import { useState, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTenant } from '../contexts/TenantContext'
import { PAPEIS, type Papel } from '../auth/roles'
import { oidcAtivo, loginOidc } from '../auth/oidc'

export default function LoginPage() {
  const { setTenant } = useTenant()
  const navigate = useNavigate()
  const [tenantId, setTenantId] = useState('')
  const [tenantName, setTenantName] = useState('')
  const [roles, setRoles] = useState<Papel[]>(['tenant-admin'])
  const [error, setError] = useState('')

  const usandoOidc = oidcAtivo()
  const sessaoExpirada = new URLSearchParams(window.location.search).has('expirado')

  function togglePapel(p: Papel) {
    setRoles((atual) => (atual.includes(p) ? atual.filter((r) => r !== p) : [...atual, p]))
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!tenantId.trim()) {
      setError('Informe o Tenant ID.')
      return
    }
    if (roles.length === 0) {
      setError('Selecione ao menos um papel.')
      return
    }
    setTenant(tenantId.trim(), tenantName.trim() || undefined, roles)
    navigate('/dashboard')
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-card__logo">
          <div className="login-card__logo-icon" aria-hidden="true">🏛️</div>
          <h1>Ponto Municipal</h1>
          <p>Sistema de Ponto Eletrônico para Servidores Públicos</p>
        </div>

        {sessaoExpirada && (
          <div role="status" style={{ marginBottom: 16, padding: '10px 14px', border: '1px solid var(--color-warning)', borderRadius: 4, fontSize: 14 }}>
            Sua sessão expirou por inatividade. Entre novamente para continuar.
          </div>
        )}

        {import.meta.env.PROD && !usandoOidc && (
          <div role="alert" style={{ marginBottom: 16, padding: '10px 14px', border: '1px solid var(--color-danger)', borderRadius: 4, color: 'var(--color-danger)', fontSize: 13 }}>
            <strong>Build de produção sem Keycloak.</strong> Defina <code>VITE_OIDC_AUTHORITY</code> e
            <code> VITE_OIDC_CLIENT_ID</code> no build — sem isso o painel cai no login de
            desenvolvimento (inseguro para produção).
          </div>
        )}

        {usandoOidc ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <button
              type="button"
              className="br-button br-button--primary br-button--block"
              onClick={() => { void loginOidc() }}
            >
              🔐 Entrar com Keycloak
            </button>
            <div className="login-dev-note">
              Você será redirecionado para o provedor de identidade do ente. Após o login, o painel
              carrega automaticamente com o seu órgão e os seus papéis.
            </div>
          </div>
        ) : (
        <form onSubmit={handleSubmit} noValidate>
          <div className="br-form-group">
            <label className="br-label br-label--required" htmlFor="tenantId">
              Tenant ID (X-Tenant-Id)
            </label>
            <input
              id="tenantId"
              type="text"
              className="br-input"
              value={tenantId}
              onChange={(e) => { setTenantId(e.target.value); setError('') }}
              placeholder="ex: 550e8400-e29b-41d4-a716-446655440000"
              required
              aria-describedby="tenantId-help"
              autoFocus
            />
            <p id="tenantId-help" className="br-form-help">
              UUID exibido pelo DevDataSeeder nos logs do backend ao inicializar.
            </p>
          </div>

          <div className="br-form-group">
            <label className="br-label" htmlFor="tenantName">
              Nome do Ente (opcional)
            </label>
            <input
              id="tenantName"
              type="text"
              className="br-input"
              value={tenantName}
              onChange={(e) => setTenantName(e.target.value)}
              placeholder="ex: Prefeitura de Exemplo"
            />
          </div>

          <fieldset style={{ border: '1px solid var(--color-gray-8)', borderRadius: 4, padding: '10px 14px', marginBottom: 12 }}>
            <legend style={{ fontSize: 13, fontWeight: 600, padding: '0 4px' }}>Papel (simulação dev)</legend>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {PAPEIS.map((p) => (
                <label key={p.value} style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 14, cursor: 'pointer' }}>
                  <input type="checkbox" checked={roles.includes(p.value)} onChange={() => togglePapel(p.value)} />
                  {p.label}
                </label>
              ))}
            </div>
            <p className="br-form-help" style={{ marginTop: 6 }}>
              Em produção os papéis virão do login Keycloak. Aqui, simule para ver o menu mudar.
            </p>
          </fieldset>

          {error && (
            <p role="alert" style={{ color: 'var(--color-danger)', fontSize: 14, marginBottom: 12 }}>
              {error}
            </p>
          )}

          <button type="submit" className="br-button br-button--primary br-button--block">
            Acessar painel
          </button>
        </form>
        )}

        {!usandoOidc && (
          <div className="login-dev-note">
            <strong>Modo desenvolvimento</strong> — autenticação simplificada via Tenant ID.<br />
            Em produção, será substituído pelo login do ente via <strong>Keycloak OIDC</strong> (sem gov.br).
          </div>
        )}
      </div>
    </div>
  )
}
