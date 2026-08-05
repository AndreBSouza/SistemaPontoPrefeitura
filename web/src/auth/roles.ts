/**
 * Papéis (roles) e o mapa de autorização do painel.
 *
 * Fonte dos papéis: em DEV, o operador escolhe no /login (guardado no localStorage);
 * em PRODUÇÃO, virão do JWT do Keycloak (claim realm_access.roles) — a decodificação entra
 * no seam do api/client. A autorização REAL é do backend (SecurityConfig, hasAnyRole); este
 * mapa apenas espelha isso na UI (esconde o que o papel não pode usar).
 */
export type Papel = 'rh' | 'gestor' | 'controladoria' | 'tenant-admin' | 'operador'

export const PAPEIS: Array<{ value: Papel; label: string }> = [
  { value: 'tenant-admin', label: 'Administrador do ente' },
  { value: 'rh', label: 'RH' },
  { value: 'gestor', label: 'Gestor / Chefia' },
  { value: 'controladoria', label: 'Controladoria / Controle interno' },
  { value: 'operador', label: 'Operador (provedor do SaaS)' },
]

/** true se o usuário tem ao menos um dos papéis permitidos. Sem restrição (undefined/[]) = liberado. */
export function podeAcessar(roles: Papel[], permitidos?: Papel[]): boolean {
  if (!permitidos || permitidos.length === 0) return true
  return roles.some((r) => permitidos.includes(r))
}

/** Papéis permitidos por rota (1º segmento do path). Espelha os matchers do SecurityConfig. */
export const ROTA_PAPEIS: Record<string, Papel[]> = {
  dashboard: [], // qualquer usuário do painel
  orgaos: ['rh', 'controladoria', 'tenant-admin'],
  servidores: ['rh', 'controladoria', 'tenant-admin'],
  jornadas: ['rh', 'controladoria', 'tenant-admin'],
  escalas: ['rh', 'controladoria', 'tenant-admin'],
  calendario: ['rh', 'tenant-admin'], // publicação de feriados/facultativos é do RH/admin
  atestados: ['gestor', 'rh', 'controladoria', 'tenant-admin'],
  correcoes: ['gestor', 'rh', 'tenant-admin'],
  ferias: ['gestor', 'rh', 'tenant-admin'],
  delegacoes: ['gestor', 'rh', 'tenant-admin'],
  espelho: ['gestor', 'rh', 'controladoria', 'tenant-admin'],
  'banco-horas': ['gestor', 'rh', 'controladoria', 'tenant-admin'],
  projetos: ['rh', 'controladoria', 'tenant-admin'],
  relatorios: ['controladoria', 'rh', 'tenant-admin'],
  conformidade: ['controladoria', 'rh', 'tenant-admin'],
  bi: ['controladoria', 'rh', 'tenant-admin'],
  adesao: ['rh', 'controladoria', 'tenant-admin'],
  comunicados: ['rh', 'tenant-admin'],
  integracoes: ['rh', 'tenant-admin'],
  auditoria: ['controladoria', 'rh', 'tenant-admin'],
  totem: ['operador', 'rh', 'tenant-admin'],
  identidade: ['rh', 'tenant-admin'],
  funcionalidades: ['rh', 'tenant-admin'],
  // Área do operador (SaaS) — ver #6 (console do operador)
  contrato: ['operador', 'tenant-admin'],
  'onboarding-entes': ['operador'],
}

/** Papéis permitidos para a rota derivada de um path como "/contrato" ou "contrato". */
export function papeisDaRota(path: string): Papel[] | undefined {
  const seg = path.replace(/^\//, '').split('/')[0]
  return ROTA_PAPEIS[seg]
}
