import 'package:dio/dio.dart';

// ---------------------------------------------------------------------------
// Modelos de resposta
// ---------------------------------------------------------------------------

/// Comprovante do endpoint legado POST /api/registros.
class Comprovante {
  final String id;
  final int nsr;
  final String tipo;
  final DateTime dataHoraServidor;

  Comprovante({
    required this.id,
    required this.nsr,
    required this.tipo,
    required this.dataHoraServidor,
  });

  factory Comprovante.fromJson(Map<String, dynamic> json) => Comprovante(
        id: json['id'] as String,
        nsr: (json['nsr'] as num).toInt(),
        tipo: json['tipo'] as String,
        dataHoraServidor: DateTime.parse(json['dataHoraServidor'] as String),
      );
}

/// Resposta do botão único POST /api/registros/bater.
class BatidaResponse {
  final String id;
  final int nsr;
  final String vinculoId;
  final String tipo;
  final String rotuloTipo;
  final String origem;
  final DateTime dataHoraServidor;
  final String horaLocal;
  final String mensagem;
  final bool offline;
  final bool foraDaCerca;

  BatidaResponse({
    required this.id,
    required this.nsr,
    required this.vinculoId,
    required this.tipo,
    required this.rotuloTipo,
    required this.origem,
    required this.dataHoraServidor,
    required this.horaLocal,
    required this.mensagem,
    required this.offline,
    this.foraDaCerca = false,
  });

  factory BatidaResponse.fromJson(Map<String, dynamic> json) => BatidaResponse(
        id: json['id'] as String,
        nsr: (json['nsr'] as num).toInt(),
        vinculoId: json['vinculoId'] as String,
        tipo: json['tipo'] as String,
        rotuloTipo: json['rotuloTipo'] as String? ?? '',
        origem: json['origem'] as String? ?? 'MOBILE',
        dataHoraServidor: DateTime.parse(json['dataHoraServidor'] as String),
        horaLocal: json['horaLocal'] as String? ?? '',
        mensagem: json['mensagem'] as String? ?? '',
        offline: json['offline'] as bool? ?? false,
        foraDaCerca: json['foraDaCerca'] as bool? ?? false,
      );
}

// ---------------------------------------------------------------------------
// Modelos de espelho / banco de horas / notificações / justificativas
// ---------------------------------------------------------------------------

class EspelhoDia {
  final String data;
  final int minutosTrabalhados;
  final int minutosEsperados;
  final bool diaUtil;
  final bool justificado;

  EspelhoDia({
    required this.data,
    required this.minutosTrabalhados,
    required this.minutosEsperados,
    required this.diaUtil,
    required this.justificado,
  });

  factory EspelhoDia.fromJson(Map<String, dynamic> json) => EspelhoDia(
        data: json['data'] as String? ?? '',
        minutosTrabalhados: (json['minutosTrabalhados'] as num?)?.toInt() ?? 0,
        minutosEsperados: (json['minutosEsperados'] as num?)?.toInt() ?? 0,
        diaUtil: json['diaUtil'] as bool? ?? false,
        justificado: json['justificado'] as bool? ?? false,
      );
}

class EspelhoResponse {
  final String vinculoId;
  final String competencia;
  final String status;
  final int totalMinutosTrabalhados;
  final int totalMinutosEsperados;
  final List<EspelhoDia> dias;

  EspelhoResponse({
    required this.vinculoId,
    required this.competencia,
    required this.status,
    required this.totalMinutosTrabalhados,
    required this.totalMinutosEsperados,
    required this.dias,
  });

  factory EspelhoResponse.fromJson(Map<String, dynamic> json) => EspelhoResponse(
        vinculoId: json['vinculoId'] as String? ?? '',
        competencia: json['competencia'] as String? ?? '',
        status: json['status'] as String? ?? '',
        totalMinutosTrabalhados:
            (json['totalMinutosTrabalhados'] as num?)?.toInt() ?? 0,
        totalMinutosEsperados:
            (json['totalMinutosEsperados'] as num?)?.toInt() ?? 0,
        dias: (json['dias'] as List<dynamic>? ?? [])
            .map((e) => EspelhoDia.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

class SaldoResponse {
  final String vinculoId;
  final int saldoMinutos;

  SaldoResponse({required this.vinculoId, required this.saldoMinutos});

  factory SaldoResponse.fromJson(Map<String, dynamic> json) => SaldoResponse(
        vinculoId: json['vinculoId'] as String? ?? '',
        saldoMinutos: (json['saldoMinutos'] as num?)?.toInt() ?? 0,
      );
}

/// Resumo "a seu favor": saldo de banco de horas + hora extra da semana corrente.
class ResumoMe {
  final int saldoBancoHorasMinutos;
  final int horaExtraSemanaMinutos;

  ResumoMe({
    required this.saldoBancoHorasMinutos,
    required this.horaExtraSemanaMinutos,
  });

  factory ResumoMe.fromJson(Map<String, dynamic> j) => ResumoMe(
        saldoBancoHorasMinutos: (j['saldoBancoHorasMinutos'] as num?)?.toInt() ?? 0,
        horaExtraSemanaMinutos: (j['horaExtraSemanaMinutos'] as num?)?.toInt() ?? 0,
      );
}

class NotificacaoItem {
  final String id;
  final String destinatario;
  final String assunto;
  final String mensagem;
  final String canal;
  final String enviadaEm;

  NotificacaoItem({
    required this.id,
    required this.destinatario,
    required this.assunto,
    required this.mensagem,
    required this.canal,
    required this.enviadaEm,
  });

  factory NotificacaoItem.fromJson(Map<String, dynamic> json) => NotificacaoItem(
        id: json['id'] as String? ?? '',
        destinatario: json['destinatario'] as String? ?? '',
        assunto: json['assunto'] as String? ?? '',
        mensagem: json['mensagem'] as String? ?? '',
        canal: json['canal'] as String? ?? '',
        enviadaEm: json['enviadaEm'] as String? ?? '',
      );
}

/// Comunicado oficial (broadcast) da prefeitura, visível ao servidor no app.
class ComunicadoItem {
  final String id;
  final String titulo;
  final String mensagem;
  final bool geral;
  final String publicadoEm;

  ComunicadoItem({
    required this.id,
    required this.titulo,
    required this.mensagem,
    required this.geral,
    required this.publicadoEm,
  });

  factory ComunicadoItem.fromJson(Map<String, dynamic> json) => ComunicadoItem(
        id: json['id'] as String? ?? '',
        titulo: json['titulo'] as String? ?? '',
        mensagem: json['mensagem'] as String? ?? '',
        geral: json['geral'] as bool? ?? true,
        publicadoEm: json['publicadoEm'] as String? ?? '',
      );
}

/// Carteira funcional digital do servidor.
class CarteiraDigital {
  final String nome;
  final String cpf;
  final String matricula;
  final String? cargo;
  final String regime;
  final String? orgao;
  final String? ente;
  final String? logoUrl;
  final String? corPrimaria;

  CarteiraDigital({
    required this.nome,
    required this.cpf,
    required this.matricula,
    this.cargo,
    required this.regime,
    this.orgao,
    this.ente,
    this.logoUrl,
    this.corPrimaria,
  });

  factory CarteiraDigital.fromJson(Map<String, dynamic> j) => CarteiraDigital(
        nome: j['nome'] as String? ?? '',
        cpf: j['cpf'] as String? ?? '',
        matricula: j['matricula'] as String? ?? '',
        cargo: j['cargo'] as String?,
        regime: j['regime'] as String? ?? '',
        orgao: j['orgao'] as String?,
        ente: j['ente'] as String?,
        logoUrl: j['logoUrl'] as String?,
        corPrimaria: j['corPrimaria'] as String?,
      );
}

/// Ausência programada do servidor (férias/licença).
class AusenciaItem {
  final String id;
  final String tipo;
  final String dataInicio;
  final String dataFim;
  final int dias;
  final String? observacao;

  AusenciaItem({
    required this.id,
    required this.tipo,
    required this.dataInicio,
    required this.dataFim,
    required this.dias,
    this.observacao,
  });

  factory AusenciaItem.fromJson(Map<String, dynamic> j) => AusenciaItem(
        id: j['id'] as String? ?? '',
        tipo: j['tipo'] as String? ?? '',
        dataInicio: j['dataInicio'] as String? ?? '',
        dataFim: j['dataFim'] as String? ?? '',
        dias: (j['dias'] as num?)?.toInt() ?? 0,
        observacao: j['observacao'] as String?,
      );
}

/// Solicitação de correção de marcação ("esqueci de bater") do servidor.
class CorrecaoItem {
  final String id;
  final String dataHora;
  final String tipo;
  final String motivo;
  final String status;

  CorrecaoItem({
    required this.id,
    required this.dataHora,
    required this.tipo,
    required this.motivo,
    required this.status,
  });

  factory CorrecaoItem.fromJson(Map<String, dynamic> j) => CorrecaoItem(
        id: j['id'] as String? ?? '',
        dataHora: j['dataHora'] as String? ?? '',
        tipo: j['tipo'] as String? ?? '',
        motivo: j['motivo'] as String? ?? '',
        status: j['status'] as String? ?? '',
      );
}

/// Resumo de gestão (12.3.11): se o servidor é chefia e quantas pendências do time há.
class GestorResumo {
  final bool souGestor;
  final int pendentes;

  GestorResumo({required this.souGestor, required this.pendentes});

  factory GestorResumo.fromJson(Map<String, dynamic> j) => GestorResumo(
        souGestor: j['souGestor'] as bool? ?? false,
        pendentes: (j['pendentes'] as num?)?.toInt() ?? 0,
      );
}

/// Evento da trilha pessoal (12.1.2): o que aconteceu com meus registros e quem decidiu.
class TrilhaEventoItem {
  final String instante;
  final String categoria;
  final String titulo;
  final String detalhe;

  TrilhaEventoItem({
    required this.instante,
    required this.categoria,
    required this.titulo,
    required this.detalhe,
  });

  factory TrilhaEventoItem.fromJson(Map<String, dynamic> j) => TrilhaEventoItem(
        instante: j['instante'] as String? ?? '',
        categoria: j['categoria'] as String? ?? '',
        titulo: j['titulo'] as String? ?? '',
        detalhe: j['detalhe'] as String? ?? '',
      );
}

class JustificativaItem {
  final String id;
  final String vinculoId;
  final String tipo;
  final String dataInicio;
  final String dataFim;
  final String motivo;
  final String? anexo;
  final String status;

  JustificativaItem({
    required this.id,
    required this.vinculoId,
    required this.tipo,
    required this.dataInicio,
    required this.dataFim,
    required this.motivo,
    this.anexo,
    required this.status,
  });

  factory JustificativaItem.fromJson(Map<String, dynamic> json) =>
      JustificativaItem(
        id: json['id'] as String? ?? '',
        vinculoId: json['vinculoId'] as String? ?? '',
        tipo: json['tipo'] as String? ?? '',
        dataInicio: json['dataInicio'] as String? ?? '',
        dataFim: json['dataFim'] as String? ?? '',
        motivo: json['motivo'] as String? ?? '',
        anexo: json['anexo'] as String?,
        status: json['status'] as String? ?? '',
      );
}

class RegistroItem {
  final String id;
  final int nsr;
  final String tipo;
  final String rotuloTipo;
  final String horaLocal;
  final String dataLocal;
  final String dataHoraServidor;
  /// SHA-256 da marcacao exigido no comprovante do REP-P (art. 79, VIII).
  final String codigoHash;

  RegistroItem({
    required this.id,
    required this.nsr,
    required this.tipo,
    required this.rotuloTipo,
    required this.horaLocal,
    required this.dataLocal,
    required this.dataHoraServidor,
    this.codigoHash = '',
  });

  factory RegistroItem.fromJson(Map<String, dynamic> json) {
    final instante = json['dataHoraServidor'] as String? ?? '';
    return RegistroItem(
      id: json['id'] as String? ?? '',
      nsr: (json['nsr'] as num?)?.toInt() ?? 0,
      tipo: json['tipo'] as String? ?? '',
      rotuloTipo: json['rotuloTipo'] as String? ?? json['tipo'] as String? ?? '',
      horaLocal: json['horaLocal'] as String? ?? _horaLocalDe(instante),
      // O backend manda dataLocal pronta; o fallback converte o Instant UTC para o fuso do
      // aparelho (exibir o UTC cru fazia batidas noturnas aparecerem no dia seguinte).
      dataLocal: json['dataLocal'] as String? ?? _dataLocalDe(instante),
      dataHoraServidor: instante,
      codigoHash: json['codigoHash'] as String? ?? '',
    );
  }

  static DateTime? _local(String instante) {
    if (instante.isEmpty) return null;
    return DateTime.tryParse(instante)?.toLocal();
  }

  static String _horaLocalDe(String instante) {
    final d = _local(instante);
    if (d == null) return '';
    return '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
  }

  static String _dataLocalDe(String instante) {
    final d = _local(instante);
    if (d == null) return '';
    return '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';
  }
}

// ---------------------------------------------------------------------------
// Cliente HTTP
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Modelos de ativação e agenda
// ---------------------------------------------------------------------------

/// Resposta da ativação do dispositivo.
class AtivacaoResult {
  final String deviceToken;
  final String tenantId;
  final String vinculoId;
  final String dispositivoId;
  final String nomeDispositivo;

  AtivacaoResult({
    required this.deviceToken,
    required this.tenantId,
    required this.vinculoId,
    required this.dispositivoId,
    required this.nomeDispositivo,
  });

  factory AtivacaoResult.fromJson(Map<String, dynamic> json) => AtivacaoResult(
        deviceToken: json['deviceToken'] as String,
        tenantId: json['tenantId'] as String,
        vinculoId: json['vinculoId'] as String,
        dispositivoId: json['dispositivoId'] as String,
        nomeDispositivo: json['nomeDispositivo'] as String? ?? '',
      );
}

/// Horário de um dia da semana (ISO: 1=Seg … 7=Dom).
class HorarioDia {
  final int diaSemana;
  final String horaEntrada; // "HH:mm:ss"
  final String horaSaida;   // "HH:mm:ss"

  HorarioDia({
    required this.diaSemana,
    required this.horaEntrada,
    required this.horaSaida,
  });

  factory HorarioDia.fromJson(Map<String, dynamic> json) => HorarioDia(
        diaSemana: (json['diaSemana'] as num).toInt(),
        horaEntrada: json['horaEntrada'] as String? ?? '08:00:00',
        horaSaida: json['horaSaida'] as String? ?? '17:00:00',
      );
}

// ---------------------------------------------------------------------------
// Cliente HTTP
// ---------------------------------------------------------------------------

/// Dados do titular para o autoatendimento LGPD (direito de acesso).
class DadosTitular {
  final String nome;
  final String cpf;
  final String? email;
  final int totalVinculos;
  final int totalRegistros;

  DadosTitular({
    required this.nome,
    required this.cpf,
    this.email,
    required this.totalVinculos,
    required this.totalRegistros,
  });

  factory DadosTitular.fromJson(Map<String, dynamic> j) => DadosTitular(
        nome: j['nome'] as String? ?? '',
        cpf: j['cpf'] as String? ?? '',
        email: j['email'] as String?,
        totalVinculos: j['totalVinculos'] as int? ?? 0,
        totalRegistros: j['totalRegistros'] as int? ?? 0,
      );
}

/// Identidade visual do ente (white-label).
class BrandingApp {
  final String nomeApp;
  final String? logoUrl;
  final String corPrimaria;
  final String corAcento;

  BrandingApp({
    required this.nomeApp,
    this.logoUrl,
    required this.corPrimaria,
    required this.corAcento,
  });

  factory BrandingApp.fromJson(Map<String, dynamic> j) => BrandingApp(
        nomeApp: j['nomeApp'] as String? ?? 'Ponto Municipal',
        logoUrl: j['logoUrl'] as String?,
        corPrimaria: j['corPrimaria'] as String? ?? '#1351B4',
        corAcento: j['corAcento'] as String? ?? '#1F6E5C',
      );
}

/// Cliente HTTP do Ponto Municipal.
///
/// Quando [deviceToken] é fornecido, envia o header X-Device-Token em toda
/// requisição (login único por aparelho). O backend resolve tenant + vínculo
/// a partir do token; um dispositivo revogado recebe HTTP 401.
///
/// Para a chamada de ativação (pré-auth) instancie sem token.
class PontoApi {
  PontoApi({required String baseUrl, String? token, String? tenant, String? deviceToken})
      : _dio = Dio(BaseOptions(
          baseUrl: baseUrl,
          headers: {
            if (token != null && token.isNotEmpty) 'Authorization': 'Bearer $token',
            // Modo dev: tenant via header. Em produção vem do claim do token.
            if (tenant != null && tenant.isNotEmpty) 'X-Tenant-Id': tenant,
            // Login único: após ativação, todas as requisições usam este header.
            if (deviceToken != null && deviceToken.isNotEmpty)
              'X-Device-Token': deviceToken,
          },
        ));

  final Dio _dio;

  // --- Botão único ---

  /// POST /api/registros/bater — o servidor deduz o tipo (entrada/intervalo/saída).
  Future<BatidaResponse> bater({
    required String vinculoId,
    required String idempotencyKey,
    double? latitude,
    double? longitude,
    bool offline = false,
    DateTime? dataHoraDispositivo,
  }) async {
    final resp = await _dio.post('/api/me/bater', data: {
      'origem': 'MOBILE',
      'idempotencyKey': idempotencyKey,
      'latitude': latitude,
      'longitude': longitude,
      'offline': offline,
      'dataHoraDispositivo':
          (dataHoraDispositivo ?? DateTime.now().toUtc()).toIso8601String(),
    });
    return BatidaResponse.fromJson(resp.data as Map<String, dynamic>);
  }

  // --- LGPD (autoatendimento do titular) ---

  /// GET /api/me/lgpd/dados — direito de acesso (exporta os próprios dados).
  Future<DadosTitular> meusDadosLgpd() async {
    final resp = await _dio.get('/api/me/lgpd/dados');
    return DadosTitular.fromJson(resp.data as Map<String, dynamic>);
  }

  /// GET /api/me/lgpd/consentimento — situação do consentimento de uma finalidade.
  Future<bool> consultarConsentimento(String finalidade) async {
    final resp = await _dio.get('/api/me/lgpd/consentimento',
        queryParameters: {'finalidade': finalidade});
    return (resp.data as Map<String, dynamic>)['concedido'] as bool? ?? false;
  }

  /// POST /api/me/lgpd/consentimento — concede ou revoga consentimento.
  Future<bool> definirConsentimento(String finalidade, bool concedido) async {
    final resp = await _dio.post('/api/me/lgpd/consentimento',
        data: {'finalidade': finalidade, 'concedido': concedido});
    return (resp.data as Map<String, dynamic>)['concedido'] as bool? ?? false;
  }

  /// GET /api/me/config — o órgão do vínculo exige verificação na batida?
  Future<bool> verificacaoObrigatoria() async {
    final resp = await _dio.get('/api/me/config');
    return (resp.data as Map<String, dynamic>)['verificacaoObrigatoria'] as bool? ?? false;
  }

  /// GET /api/me/branding — identidade visual do ente (nome, logo, cores).
  Future<BrandingApp> getBranding() async {
    final resp = await _dio.get('/api/me/branding');
    return BrandingApp.fromJson(resp.data as Map<String, dynamic>);
  }

  /// GET /api/me/carteira — carteira funcional digital do servidor.
  Future<CarteiraDigital> minhaCarteira() async {
    final resp = await _dio.get('/api/me/carteira');
    return CarteiraDigital.fromJson(resp.data as Map<String, dynamic>);
  }

  // --- Endpoint legado (mantido para compatibilidade) ---

  Future<Comprovante> registrar({
    required String vinculoId,
    required String tipo,
    required String idempotencyKey,
    double? latitude,
    double? longitude,
    bool offline = false,
  }) async {
    final resp = await _dio.post('/api/registros', data: {
      'vinculoId': vinculoId,
      'tipo': tipo,
      'origem': 'MOBILE',
      'latitude': latitude,
      'longitude': longitude,
      'offline': offline,
      'idempotencyKey': idempotencyKey,
    });
    return Comprovante.fromJson(resp.data as Map<String, dynamic>);
  }

  // --- Comprovantes ---

  Future<List<RegistroItem>> listarRegistros(String vinculoId) async {
    final resp = await _dio.get('/api/me/comprovantes');
    return (resp.data as List<dynamic>)
        .map((e) => RegistroItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // --- Espelho ---

  Future<EspelhoResponse> espelho({
    required String vinculoId,
    required String competencia, // yyyy-MM
  }) async {
    final resp = await _dio.get('/api/me/espelho', queryParameters: {
      'competencia': competencia,
    });
    return EspelhoResponse.fromJson(resp.data as Map<String, dynamic>);
  }

  // --- Banco de horas ---

  Future<SaldoResponse> saldoBancoHoras(String vinculoId) async {
    final resp = await _dio.get('/api/me/banco-horas');
    return SaldoResponse.fromJson(resp.data as Map<String, dynamic>);
  }

  /// GET /api/me/resumo — saldo de banco de horas + hora extra da semana ("a seu favor").
  Future<ResumoMe> resumo() async {
    final resp = await _dio.get('/api/me/resumo');
    return ResumoMe.fromJson(resp.data as Map<String, dynamic>);
  }

  // --- Notificações ---

  Future<List<NotificacaoItem>> listarNotificacoes(String destinatario) async {
    final resp = await _dio.get('/api/me/notificacoes');
    return (resp.data as List<dynamic>)
        .map((e) => NotificacaoItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // --- Comunicados oficiais (broadcast) ---

  /// GET /api/me/comunicados — comunicados oficiais visíveis ao servidor (gerais + do órgão).
  Future<List<ComunicadoItem>> listarComunicados() async {
    final resp = await _dio.get('/api/me/comunicados');
    return (resp.data as List<dynamic>)
        .map((e) => ComunicadoItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // --- Correção de marcação ("esqueci de bater") ---

  /// POST /api/me/correcoes — solicita correção de marcação (aprovação da chefia/RH).
  Future<CorrecaoItem> solicitarCorrecao({
    required DateTime dataHora,
    required String tipo,
    required String motivo,
  }) async {
    final resp = await _dio.post('/api/me/correcoes', data: {
      'dataHora': dataHora.toUtc().toIso8601String(),
      'tipo': tipo,
      'motivo': motivo,
    });
    return CorrecaoItem.fromJson(resp.data as Map<String, dynamic>);
  }

  /// GET /api/me/correcoes — minhas solicitações de correção.
  Future<List<CorrecaoItem>> listarCorrecoes() async {
    final resp = await _dio.get('/api/me/correcoes');
    return (resp.data as List<dynamic>)
        .map((e) => CorrecaoItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // --- App do gestor (12.3.11) ---

  /// GET /api/me/chefia — resumo de gestão (é chefia? quantas pendências do time?).
  Future<GestorResumo> chefiaResumo() async {
    final resp = await _dio.get('/api/me/chefia');
    return GestorResumo.fromJson(resp.data as Map<String, dynamic>);
  }

  /// GET /api/me/chefia/pendentes — justificativas pendentes do time do gestor.
  Future<List<JustificativaItem>> pendentesDoTime() async {
    final resp = await _dio.get('/api/me/chefia/pendentes');
    return (resp.data as List<dynamic>)
        .map((e) => JustificativaItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// POST /api/me/chefia/justificativas/{id}/aprovar — decide pelo time (com alçada).
  Future<void> aprovarDoTime(String id, {String? motivo}) async {
    await _dio.post('/api/me/chefia/justificativas/$id/aprovar',
        data: {if (motivo != null && motivo.isNotEmpty) 'motivoDecisao': motivo});
  }

  /// POST /api/me/chefia/justificativas/{id}/rejeitar.
  Future<void> rejeitarDoTime(String id, {String? motivo}) async {
    await _dio.post('/api/me/chefia/justificativas/$id/rejeitar',
        data: {if (motivo != null && motivo.isNotEmpty) 'motivoDecisao': motivo});
  }

  /// GET /api/me/trilha — meu histórico (correções e justificativas + decisões).
  Future<List<TrilhaEventoItem>> listarTrilha() async {
    final resp = await _dio.get('/api/me/trilha');
    return (resp.data as List<dynamic>)
        .map((e) => TrilhaEventoItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// GET /api/me/ausencias — minhas férias/licenças programadas.
  Future<List<AusenciaItem>> minhasAusencias() async {
    final resp = await _dio.get('/api/me/ausencias');
    return (resp.data as List<dynamic>)
        .map((e) => AusenciaItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// POST /api/me/satisfacao — avaliação de satisfação (nota 1..5 + comentário opcional).
  Future<void> avaliarSatisfacao({required int nota, String? comentario}) async {
    await _dio.post('/api/me/satisfacao', data: {
      'nota': nota,
      if (comentario != null && comentario.isNotEmpty) 'comentario': comentario,
    });
  }

  // --- Justificativas ---
  //
  // NÃO existe aqui um "listar por vinculoId": /api/justificativas é endpoint administrativo e
  // não aceita token de dispositivo. O servidor vê as próprias justificativas em /api/me/trilha.

  Future<JustificativaItem> solicitarJustificativa({
    required String vinculoId,
    required String tipo,
    required String dataInicio,
    required String dataFim,
    String? motivo,
    String? anexo,
  }) async {
    final resp = await _dio.post('/api/me/justificativas', data: {
      'tipo': tipo,
      'dataInicio': dataInicio,
      'dataFim': dataFim,
      if (motivo != null) 'motivo': motivo,
      if (anexo != null) 'anexo': anexo,
    });
    return JustificativaItem.fromJson(resp.data as Map<String, dynamic>);
  }

  // --- LGPD ---
  //
  // O consentimento do servidor é definido por definirConsentimento() em /api/me/lgpd/* (acima).
  // /api/lgpd/** é área da controladoria e responde 403 para token de dispositivo.

  // --- Ativação de dispositivo (pré-auth, sem token) ---

  /// POST /api/ativacao/ativar — troca o código de ativação por um deviceToken.
  Future<AtivacaoResult> ativar({
    required String codigo,
    String? nomeDispositivo,
  }) async {
    final resp = await _dio.post('/api/ativacao/ativar', data: {
      'codigo': codigo,
      if (nomeDispositivo != null && nomeDispositivo.isNotEmpty)
        'nomeDispositivo': nomeDispositivo,
    });
    return AtivacaoResult.fromJson(resp.data as Map<String, dynamic>);
  }

  // --- Jornada (para agendamento de lembretes) ---

  /// GET /api/me/jornada — horários da jornada vigente do próprio servidor.
  Future<List<HorarioDia>> jornada() async {
    final resp = await _dio.get('/api/me/jornada');
    return (resp.data as List<dynamic>)
        .map((e) => HorarioDia.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
