import { useState, useEffect, ReactNode } from 'react'
import { NavLink, useNavigate, useLocation } from 'react-router-dom'
import { useTenant } from '../contexts/TenantContext'
import { api } from '../api/client'
import { BrandingResponse } from '../api/types'
import { podeAcessar, papeisDaRota, ROTA_PAPEIS, PAPEIS } from '../auth/roles'
import { oidcAtivo, logoutOidc } from '../auth/oidc'

interface NavSection {
  title: string
  items: Array<{
    to: string
    icon: string
    label: string
  }>
}

const NAV: NavSection[] = [
  {
    title: 'Principal',
    items: [
      { to: '/dashboard', icon: '🏠', label: 'Dashboard' },
    ],
  },
  {
    title: 'Cadastros (P0)',
    items: [
      { to: '/orgaos', icon: '🏛️', label: 'Órgãos / Lotações' },
      { to: '/servidores', icon: '👥', label: 'Servidores' },
    ],
  },
  {
    title: 'Frequência (P1)',
    items: [
      { to: '/jornadas', icon: '🕐', label: 'Jornadas' },
      { to: '/escalas', icon: '📅', label: 'Escalas' },
      { to: '/calendario', icon: '🗓️', label: 'Calendário oficial' },
      { to: '/atestados', icon: '📋', label: 'Atestados / Justificativas' },
      { to: '/correcoes', icon: '✏️', label: 'Correções de marcação' },
      { to: '/ferias', icon: '🏖️', label: 'Férias / Licenças' },
      { to: '/delegacoes', icon: '🤝', label: 'Delegação de aprovação' },
      { to: '/espelho', icon: '📊', label: 'Espelho / Fechamento' },
      { to: '/banco-horas', icon: '⏱️', label: 'Banco de Horas' },
      { to: '/projetos', icon: '🗂️', label: 'Projetos / Convênios' },
    ],
  },
  {
    title: 'Relatórios (P1)',
    items: [
      { to: '/relatorios', icon: '📈', label: 'Frequência' },
      { to: '/conformidade', icon: '✔️', label: 'Conformidade IN-008' },
      { to: '/bi', icon: '📊', label: 'BI executivo' },
      { to: '/adesao', icon: '📶', label: 'Adesão' },
    ],
  },
  {
    title: 'Comunicação',
    items: [
      { to: '/comunicados', icon: '📣', label: 'Comunicados' },
    ],
  },
  {
    title: 'Integrações (P2)',
    items: [
      { to: '/integracoes', icon: '🔗', label: 'Folha / eSocial' },
      { to: '/auditoria', icon: '🔍', label: 'Auditoria' },
    ],
  },
  {
    title: 'Totem',
    items: [
      { to: '/totem', icon: '📟', label: 'Totem de Ponto' },
    ],
  },
  {
    title: 'Configurações',
    items: [
      { to: '/identidade', icon: '🎨', label: 'Identidade visual' },
      { to: '/funcionalidades', icon: '🎛️', label: 'Funcionalidades' },
    ],
  },
  {
    // Área do provedor do SaaS (operador) — separada da operação municipal.
    title: 'Operador (SaaS)',
    items: [
      { to: '/contrato', icon: '📄', label: 'Contrato' },
      { to: '/onboarding-entes', icon: '🏢', label: 'Adesão de entes' },
    ],
  },
]

interface AppShellProps {
  children: ReactNode
}

export function AppShell({ children }: AppShellProps) {
  const { tenantId, tenantName, roles, clearTenant } = useTenant()
  const navigate = useNavigate()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [branding, setBranding] = useState<BrandingResponse | null>(null)
  const [dark, setDark] = useState(() => localStorage.getItem('pm_theme') === 'dark')

  // Tema escuro do painel (12.2.4): aplica o atributo no <html> e persiste a escolha.
  useEffect(() => {
    document.documentElement.dataset.theme = dark ? 'dark' : ''
    localStorage.setItem('pm_theme', dark ? 'dark' : 'light')
  }, [dark])

  // White-label: carrega a identidade do ente e aplica a cor primária ao tema.
  useEffect(() => {
    if (!tenantId) return
    api
      .get<BrandingResponse>('/api/branding')
      .then((b) => {
        setBranding(b)
        const cor = b.corPrimaria.startsWith('#') ? b.corPrimaria : `#${b.corPrimaria}`
        document.documentElement.style.setProperty('--color-primary', cor)
      })
      .catch(() => {
        /* sem permissão/erro → mantém o tema padrão */
      })
  }, [tenantId])

  function handleLogout() {
    if (oidcAtivo()) {
      // Encerra a sessão no Keycloak (o token local é limpo dentro de logoutOidc).
      void logoutOidc()
      return
    }
    clearTenant()
    navigate('/login')
  }

  return (
    <>
      <a className="skip-link" href="#main-content">Pular para conteúdo principal</a>

      {/* Header institucional */}
      <header className="br-header" role="banner">
        <button
          className="sidebar-toggle"
          aria-label="Abrir menu lateral"
          aria-expanded={sidebarOpen}
          onClick={() => setSidebarOpen((v) => !v)}
        >
          ☰
        </button>
        <NavLink to="/dashboard" className="br-header__logo" aria-label="Página inicial">
          {branding?.logoUrl ? (
            <img src={branding.logoUrl} alt="" style={{ height: 28, marginRight: 8, verticalAlign: 'middle' }} />
          ) : (
            <span aria-hidden="true">🏛️ </span>
          )}
          {branding?.nomeApp ?? 'Ponto Municipal'}
          <span>Sistema de Ponto Eletrônico</span>
        </NavLink>

        <div className="br-header__tenant" aria-label="Ente ativo">
          {tenantName ? `Ente: ${tenantName}` : tenantId ? `Tenant: ${tenantId}` : ''}
        </div>

        <div className="br-header__actions">
          <button
            className="br-header__btn"
            onClick={() => setDark((v) => !v)}
            aria-label={dark ? 'Ativar tema claro' : 'Ativar tema escuro'}
            aria-pressed={dark}
          >
            {dark ? '☀️ Claro' : '🌙 Escuro'}
          </button>
          <span className="br-header__user" title={`Papéis: ${roles.join(', ')}`}>
            <span aria-hidden="true">👤</span>
            <span>{roles.map((r) => PAPEIS.find((p) => p.value === r)?.label ?? r).join(' · ')}</span>
          </span>
          <button
            className="br-header__btn"
            onClick={() => navigate('/login')}
            aria-label="Trocar ente ativo"
          >
            Trocar ente
          </button>
          <button
            className="br-header__btn"
            onClick={handleLogout}
            aria-label="Sair do sistema"
          >
            Sair
          </button>
        </div>
      </header>

      {/* Overlay para fechar sidebar no mobile */}
      {sidebarOpen && (
        <div
          style={{ position: 'fixed', inset: 0, zIndex: 99 }}
          aria-hidden="true"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <div className="app-shell">
        {/* Sidebar */}
        <nav
          className={`br-menu${sidebarOpen ? ' br-menu--open' : ''}`}
          aria-label="Menu principal"
        >
          {NAV.map((section) => {
            const itens = section.items.filter((item) =>
              podeAcessar(roles, ROTA_PAPEIS[item.to.replace(/^\//, '')]))
            if (itens.length === 0) return null
            return (
              <div key={section.title} className="br-menu__section">
                <div className="br-menu__section-title">{section.title}</div>
                {itens.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      `br-menu__item${isActive ? ' br-menu__item--active' : ''}`
                    }
                    onClick={() => setSidebarOpen(false)}
                  >
                    <span className="br-menu__icon" aria-hidden="true">{item.icon}</span>
                    {item.label}
                  </NavLink>
                ))}
              </div>
            )
          })}
        </nav>

        {/* Content — guarda por papel: bloqueia acesso direto por URL a rota sem permissão. */}
        <main id="main-content" className="main-content">
          {podeAcessar(roles, papeisDaRota(location.pathname)) ? children : (
            <div className="empty-state" style={{ padding: '48px 0' }}>
              <div className="empty-state__icon" aria-hidden="true">🔒</div>
              <p className="empty-state__title">Sem permissão para esta área</p>
              <p>Seu papel não tem acesso a esta funcionalidade. Fale com o administrador do ente.</p>
            </div>
          )}
        </main>
      </div>

      <footer className="br-footer" role="contentinfo">
        Ponto Municipal © 2024 — Sistema de Ponto Eletrônico para Servidores Públicos Municipais
      </footer>
    </>
  )
}
