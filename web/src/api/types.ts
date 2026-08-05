/** Tipos centralizados que espelham os DTOs do backend */

// ---- Tenant ----
export interface TenantResponse {
  id: string
  nome: string
  slug: string
  tipoPoder: string
  ativo: boolean
}

// ---- Lotação (Órgão) ----
export interface LotacaoResponse {
  id: string
  nome: string
  sigla: string | null
  chefiaServidorId: string | null
  jornadaPadraoId: string | null
  toleranciaMinutos: number | null
  bancoHorasHabilitado: boolean | null
  geofenceLatitude: number | null
  geofenceLongitude: number | null
  geofenceRaioMetros: number | null
  tetoBancoHorasMinutos: number | null
  verificacaoObrigatoria: boolean | null
  adaptacaoAte: string | null
  teletrabalho: boolean | null
}

export interface CriarLotacaoRequest {
  nome: string
  sigla?: string
}

export interface DefinirRegrasRequest {
  jornadaPadraoId?: string
  toleranciaMinutos?: number
  bancoHorasHabilitado?: boolean
  geofenceLatitude?: number | null
  geofenceLongitude?: number | null
  geofenceRaioMetros?: number | null
  tetoBancoHorasMinutos?: number | null
  verificacaoObrigatoria?: boolean | null
  adaptacaoAte?: string | null
  teletrabalho?: boolean | null
}

// ---- Multi-geofence: áreas de referência adicionais do órgão (locais volantes) ----
export interface GeofenceLocal {
  id: string
  lotacaoId: string
  nome: string
  latitude: number
  longitude: number
  raioMetros: number
}

export interface CriarGeofenceLocalRequest {
  nome: string
  latitude: number
  longitude: number
  raioMetros: number
}

// ---- Onboarding self-service do ente ----
export interface SolicitacaoEnte {
  id: string
  nome: string
  slug: string
  tipoPoder: string
  responsavelNome: string
  responsavelEmail: string
  status: string
  tenantId: string | null
  criadoEm: string
}

export interface OnboardingProvisionado {
  tenantId: string
  slug: string
  lotacaoId: string | null
}

// ---- Sobreaviso (on-call) ----
export interface SobreavisoResponse {
  id: string
  vinculoId: string
  data: string
  minutos: number
  observacao: string | null
}

export interface RegistrarSobreavisoRequest {
  vinculoId: string
  data: string
  minutos: number
  observacao?: string
}

// ---- Registro de ponto (visão do administrador) ----
export interface RegistroComprovante {
  id: string
  nsr: number
  vinculoId: string
  tipo: string
  origem: string
  dataHoraServidor: string
  offline: boolean
  latitude: number | null
  longitude: number | null
  foraDaCerca: boolean
}

export interface BrandingResponse {
  nomeEnte: string
  nomeApp: string
  logoUrl: string | null
  corPrimaria: string
  corAcento: string
  cnpj: string | null
}

export interface DefinirBrandingRequest {
  nomeApp?: string
  logoUrl?: string | null
  corPrimaria?: string
  corAcento?: string
}

// ---- Servidor / Vínculo ----
export interface VinculoResponse {
  id: string
  matricula: string
  regime: string
  cargo: string | null
  cargaHorariaSemanal: number | null
  lotacaoId: string | null
}

export interface ServidorResponse {
  id: string
  cpf: string
  nome: string
  email: string | null
  ativo: boolean
  vinculos: VinculoResponse[]
}

export interface CriarVinculoRequest {
  matricula: string
  regime: string
  cargo?: string
  cargaHorariaSemanal?: number
  lotacaoId?: string
}

export interface CriarServidorRequest {
  cpf: string
  nome: string
  email?: string
  vinculos: CriarVinculoRequest[]
}

export interface ImportacaoResultado {
  totalLinhas: number
  importados: number
  erros: Array<{ linha: number; mensagem: string }>
}

// ---- Jornada ----
export interface JornadaResponse {
  id: string
  nome: string
  tipo: string
  cargaHorariaSemanalMin: number
  toleranciaMin: number
  intervaloMin: number
  horaAtividadeMin: number | null
  ativo: boolean
}

export interface CriarJornadaRequest {
  nome: string
  tipo: string
  cargaHorariaSemanalMin: number
  toleranciaMin: number
  intervaloMin: number
  horaAtividadeMin?: number | null
}

export interface PisoMagisterio {
  jornadaId: string
  nome: string
  cargaHorariaSemanalMin: number
  horaAtividadeMin: number
  minimoLegalMin: number
  percentual: number
  atendePiso: boolean
}

export interface HorarioResponse {
  diaSemana: number
  horaEntrada: string
  horaSaida: string
}

export interface HorarioRequest {
  diaSemana: number
  horaEntrada: string
  horaSaida: string
}

// ---- Escala ----
export interface EscalaResponse {
  id: string
  vinculoId: string
  jornadaId: string
  dataInicio: string
  dataFim: string | null
}

export interface CriarEscalaRequest {
  vinculoId: string
  jornadaId: string
  dataInicio: string
  dataFim?: string
}

// ---- Justificativas ----
export interface JustificativaResponse {
  id: string
  vinculoId: string
  tipo: string
  dataInicio: string
  dataFim: string
  motivo: string | null
  anexo: string | null
  status: string
  motivoDecisao: string | null
}

export interface SolicitarJustificativaRequest {
  vinculoId: string
  tipo: string
  dataInicio: string
  dataFim: string
  motivo?: string
}

export interface DecisaoLoteRequest {
  ids: string[]
  motivoDecisao?: string
}

// ---- BI executivo (12.3.2) + simulador/LRF (12.5.5/12.5.1) ----
export interface OrgaoBi {
  lotacaoId: string
  orgao: string
  vinculos: number
  faltas: number
  horaExtraMinutos: number
  fantasmas: number
}

export interface BiExecutivoResponse {
  competencia: string
  totalVinculos: number
  totalFaltas: number
  totalHoraExtraMinutos: number
  totalFantasmas: number
  orgaos: OrgaoBi[]
}

export interface RoiResponse {
  competencia: string
  custoHoraExtraAtualReais: number
  economiaHoraExtraReais: number
  custoAbsenteismoAtualReais: number
  economiaAbsenteismoReais: number
  economiaTotalEstimadaReais: number
}

export interface SimulacaoResponse {
  custoHoraExtraReais: number
  gastoProjetadoReais: number
  percentualRclProjetado: number
  risco: 'OK' | 'ALERTA' | 'PRUDENCIAL' | 'ESTOURO'
  limitePercentual: number
  prudencialPercentual: number
}

// ---- Férias e licenças (12.4.1) ----
export type TipoAusencia =
  | 'FERIAS'
  | 'LICENCA_MEDICA'
  | 'LICENCA_MATERNIDADE'
  | 'LICENCA_PATERNIDADE'
  | 'LICENCA_PREMIO'
  | 'LICENCA_NOJO'
  | 'VIAGEM'
  | 'CAPACITACAO'
  | 'OUTRA'

export interface ResumoSatisfacao {
  total: number
  media: number
  distribuicao: Record<string, number>
  comentarios: string[]
}

export interface AusenciaResponse {
  id: string
  vinculoId: string
  tipo: TipoAusencia
  dataInicio: string
  dataFim: string
  dias: number
  observacao: string | null
}

export interface AgendarAusenciaRequest {
  vinculoId: string
  tipo: TipoAusencia
  dataInicio: string
  dataFim: string
  observacao?: string
}

export interface CoberturaItem {
  vinculoId: string
  servidor: string
  tipo: TipoAusencia
  dataInicio: string
  dataFim: string
}

export interface CoberturaResponse {
  lotacaoId: string
  competencia: string
  totalVinculos: number
  ausencias: CoberturaItem[]
}

// ---- Delegação de aprovação (12.6.8) ----
export interface DelegacaoResponse {
  id: string
  deleganteServidorId: string
  delegadoServidorId: string
  dataInicio: string
  dataFim: string
  ativo: boolean
}

export interface CriarDelegacaoRequest {
  deleganteServidorId: string
  delegadoServidorId: string
  dataInicio: string
  dataFim: string
}

// ---- Correção de marcação (12.1.4 / 12.6.4) ----
export type TipoMarcacao = 'ENTRADA' | 'SAIDA' | 'INTERVALO_INICIO' | 'INTERVALO_FIM'

export interface CorrecaoResponse {
  id: string
  vinculoId: string
  dataHora: string
  tipo: TipoMarcacao
  motivo: string
  status: 'PENDENTE' | 'APROVADA' | 'REJEITADA'
  registroId: string | null
  motivoDecisao: string | null
  solicitadoEm: string
  decididoEm: string | null
}

export interface CorrecaoLoteItem {
  dataHora: string
  tipo: TipoMarcacao
}

export interface CorrecaoLoteRequest {
  vinculoId: string
  itens: CorrecaoLoteItem[]
  motivo?: string
}

// ---- Espelho ----
export interface ApuracaoDiaResponse {
  vinculoId: string
  data: string
  minutosTrabalhados: number
  minutosEsperados: number
  diaUtil: boolean
  justificado: boolean
  emAdaptacao: boolean
  ocorrencias: Array<{ tipo: string; minutos: number }>
}

// ---- Fechamento em lote / pendências (12.6.2) ----
export interface PendenciaItem {
  vinculoId: string
  matricula: string
  servidorId: string
  servidor: string
}

export interface OrgaoPendencia {
  lotacaoId: string | null
  orgao: string
  pendentes: number
  itens: PendenciaItem[]
}

export interface PendenciaFechamentoResponse {
  competencia: string
  totalVinculos: number
  fechadas: number
  pendentes: number
  orgaos: OrgaoPendencia[]
}

export interface EspelhoResponse {
  vinculoId: string
  competencia: string
  status: string
  cienciaEm: string | null
  totalMinutosTrabalhados: number
  totalMinutosEsperados: number
  dias: ApuracaoDiaResponse[]
}

// ---- Banco de horas ----
export interface SaldoResponse {
  vinculoId: string
  saldoMinutos: number
}

// ---- Relatórios ----
export interface RelatorioFrequenciaResponse {
  vinculoId: string
  competencia: string
  totalMinutosTrabalhados: number
  totalMinutosEsperados: number
  qtdAtrasos: number
  qtdFaltas: number
  minutosHoraExtra: number
  diasJustificados: number
}

export interface ConformidadeResponse {
  competencia: string
  totalServidores: number
  totalVinculos: number
  totalRegistros: number
  descricao: string
}

// ---- Dossiê de conformidade / escudo TCM (12.1.7 / 12.5.6 / 12.3.4 / 12.3.5) ----
export interface DossieResponse {
  competencia: string
  conformidade: ConformidadeResponse
  afdHashSha256: string
  afdTotalRegistros: number
  cadeiaIntegra: boolean
  integridadeDetalhe: string
  indicadores: {
    totalVinculos: number
    dispositivosAtivos: number
    adesao: number
    registrosNoPeriodo: number
  }
  prazoSubmissao: string
  diasRestantes: number
  escudos: string[]
}

// ---- Adesão por órgão/regime (12.1.9 / 12.1.8) ----
export interface AdesaoGrupo {
  chave: string
  rotulo: string
  vinculos: number
  aderiram: number
  percentual: number
}

export interface AdesaoResponse {
  grupos: AdesaoGrupo[]
}

// ---- Inconsistências de jornada (12.4.15) ----
export interface InconsistenciaItem {
  vinculoId: string
  servidor: string
  data: string
  marcacoes: number
  motivo: string
}

export interface InconsistenciasResponse {
  competencia: string
  inconsistencias: InconsistenciaItem[]
}

// ---- Relatório de abonos (12.6.15) ----
export interface AbonoResponse {
  id: string
  vinculoId: string
  servidor: string
  tipo: string
  dataInicio: string
  dataFim: string
  status: string
  motivoDecisao: string | null
  decididoEm: string | null
  aprovadorId: string | null
}

// ---- Detecção de irregularidades (12.5.2) ----
export interface AcumuloItem {
  servidorId: string
  servidor: string
  vinculoA: string
  vinculoB: string
}

export interface FantasmaItem {
  servidorId: string
  servidor: string
  vinculoId: string
  matricula: string
}

export interface DeteccaoResponse {
  acumulos: AcumuloItem[]
  fantasmas: FantasmaItem[]
}

// ---- Alertas de risco proativos (12.6.10) ----
export interface AjusteManualAlerta {
  vinculoId: string
  servidor: string
  data: string
  minutos: number
  descricao: string
}

export interface ForaDaCercaAlerta {
  vinculoId: string
  servidor: string
  quantidade: number
}

export interface AlertasResponse {
  ajustesManuais: AjusteManualAlerta[]
  batidasForaDaCerca: ForaDaCercaAlerta[]
}

// ---- Calendário oficial (12.4.5 / 12.1.6) ----
export type TipoEventoCalendario = 'FERIADO' | 'PONTO_FACULTATIVO' | 'ABONO_COLETIVO'

export interface EventoCalendarioResponse {
  id: string
  data: string
  tipo: TipoEventoCalendario
  descricao: string
  lotacaoId: string | null
  geral: boolean
}

export interface CriarEventoRequest {
  data: string
  tipo: TipoEventoCalendario
  descricao: string
  lotacaoId?: string | null
}

// ---- Comunicados oficiais (12.3.7) ----
export interface ComunicadoResponse {
  id: string
  titulo: string
  mensagem: string
  lotacaoId: string | null
  geral: boolean
  publicadoEm: string
}

export interface PublicarComunicadoRequest {
  titulo: string
  mensagem: string
  lotacaoId?: string | null
}

// ---- Billing ----
// ---- Contrato (substitui billing por demanda; contratação pública = valor fixo) ----
export type ModalidadeContratacao =
  | 'DISPENSA' | 'PREGAO' | 'INEXIGIBILIDADE' | 'CONCORRENCIA' | 'ADESAO_ATA' | 'OUTRA'

export interface ContratoResponse {
  id: string
  modalidade: ModalidadeContratacao
  modalidadeRotulo: string
  numeroProcesso: string | null
  empenho: string | null
  vigenciaInicio: string
  vigenciaFim: string
  valorGlobal: number | null
  valorMensal: number | null
  observacao: string | null
  vigente: boolean
}

export interface ContratoRequest {
  modalidade: ModalidadeContratacao
  numeroProcesso?: string
  empenho?: string
  vigenciaInicio: string
  vigenciaFim: string
  valorGlobal?: number
  valorMensal?: number
  observacao?: string
}

export interface ExecucaoContratoResponse {
  competencia: string
  contratoVigente: boolean
  valorMensal: number | null
  servidoresAtivos: number
  dispositivosAtivos: number
  registrosNoPeriodo: number
  numeroProcesso: string | null
  empenho: string | null
}

// ---- Auditoria ----
export interface AuditoriaResponse {
  id: string
  acao: string
  entidade: string
  entidadeId: string
  ator: string
  detalhe: string | null
  ocorridoEm: string
}

// ---- Ativação de Dispositivo ----
export interface GerarCodigoRequest {
  vinculoId: string
  validadeHoras?: number
}

export interface GerarCodigoResponse {
  codigo: string
  vinculoId: string
  expiraEm: string
}

export interface DispositivoResponse {
  id: string
  vinculoId: string
  nome: string | null
  ativo: boolean
  criadoEm: string
}

// ---- Funcionalidades (feature flags ligáveis pelo painel admin) ----
export interface FuncionalidadeResponse {
  chave: string
  rotulo: string
  habilitado: boolean
}

// ---- Anomalias heurísticas (ligável por FuncionalidadeResponse ANOMALIAS) ----
export interface AnomaliaItem {
  tipo: string
  vinculoId: string
  servidor: string
  detalhe: string
}
export interface AnomaliaResponse {
  habilitada: boolean
  anomalias: AnomaliaItem[]
}
