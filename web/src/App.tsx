import { BrowserRouter, Route, Routes, Navigate, useLocation } from 'react-router-dom'
import { TenantProvider, useTenant } from './contexts/TenantContext'
import { AppShell } from './layout/AppShell'

// Pages
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import OrgaosPage from './pages/OrgaosPage'
import ServidoresPage from './pages/ServidoresPage'
import JornadasPage from './pages/JornadasPage'
import EscalasPage from './pages/EscalasPage'
import AdesaoPage from './pages/AdesaoPage'
import AtestadosPage from './pages/AtestadosPage'
import BiExecutivoPage from './pages/BiExecutivoPage'
import CalendarioPage from './pages/CalendarioPage'
import ComunicadosPage from './pages/ComunicadosPage'
import CorrecoesPage from './pages/CorrecoesPage'
import DelegacoesPage from './pages/DelegacoesPage'
import EspelhoPage from './pages/EspelhoPage'
import FeriasPage from './pages/FeriasPage'
import BancoHorasPage from './pages/BancoHorasPage'
import RelatoriosPage from './pages/RelatoriosPage'
import ConformidadePage from './pages/ConformidadePage'
import IntegracoesPage from './pages/IntegracoesPage'
import ProjetosPage from './pages/ProjetosPage'
import AuditoriaPage from './pages/AuditoriaPage'
import ContratoPage from './pages/ContratoPage'
import IdentidadeVisualPage from './pages/IdentidadeVisualPage'
import FuncionalidadesPage from './pages/FuncionalidadesPage'
import TotemPage from './TotemPage'
import ServidorPontoPage from './pages/ServidorPontoPage'
import AderirPage from './pages/AderirPage'
import OnboardingEntesPage from './pages/OnboardingEntesPage'
import CallbackPage from './pages/CallbackPage'

/**
 * Guard: redireciona para /login se não houver tenant configurado.
 * Em produção substituir por check do bearer token Keycloak.
 */
function RequireTenant({ children }: { children: React.ReactNode }) {
  const { tenantId } = useTenant()
  const location = useLocation()

  if (!tenantId) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  return <>{children}</>
}

function AdminRoutes() {
  return (
    <RequireTenant>
      <AppShell>
        <Routes>
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="orgaos" element={<OrgaosPage />} />
          <Route path="servidores" element={<ServidoresPage />} />
          <Route path="jornadas" element={<JornadasPage />} />
          <Route path="escalas" element={<EscalasPage />} />
          <Route path="calendario" element={<CalendarioPage />} />
          <Route path="atestados" element={<AtestadosPage />} />
          <Route path="correcoes" element={<CorrecoesPage />} />
          <Route path="ferias" element={<FeriasPage />} />
          <Route path="delegacoes" element={<DelegacoesPage />} />
          <Route path="espelho" element={<EspelhoPage />} />
          <Route path="banco-horas" element={<BancoHorasPage />} />
          <Route path="projetos" element={<ProjetosPage />} />
          <Route path="relatorios" element={<RelatoriosPage />} />
          <Route path="conformidade" element={<ConformidadePage />} />
          <Route path="bi" element={<BiExecutivoPage />} />
          <Route path="adesao" element={<AdesaoPage />} />
          <Route path="integracoes" element={<IntegracoesPage />} />
          <Route path="auditoria" element={<AuditoriaPage />} />
          <Route path="comunicados" element={<ComunicadosPage />} />
          <Route path="contrato" element={<ContratoPage />} />
          <Route path="identidade" element={<IdentidadeVisualPage />} />
          <Route path="funcionalidades" element={<FuncionalidadesPage />} />
          <Route path="onboarding-entes" element={<OnboardingEntesPage />} />
          <Route path="totem" element={<TotemPage />} />
          {/* Fallback dentro do admin */}
          <Route path="*" element={<Navigate to="dashboard" replace />} />
        </Routes>
      </AppShell>
    </RequireTenant>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <TenantProvider>
        <Routes>
          {/* Rota pública */}
          <Route path="/login" element={<LoginPage />} />
          {/* Retorno do Keycloak (OIDC) */}
          <Route path="/callback" element={<CallbackPage />} />
          {/* Raiz → redireciona para dashboard */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          {/* Totem legado → mantém rota */}
          <Route path="/totem-standalone" element={<TotemPage />} />
          {/* Acesso do servidor (bater ponto pela web) — público; autentica por código/dispositivo */}
          <Route path="/servidor" element={<ServidorPontoPage />} />
          {/* Adesão self-service (público) */}
          <Route path="/aderir" element={<AderirPage />} />
          {/* Rotas protegidas */}
          <Route path="/*" element={<AdminRoutes />} />
        </Routes>
      </TenantProvider>
    </BrowserRouter>
  )
}
