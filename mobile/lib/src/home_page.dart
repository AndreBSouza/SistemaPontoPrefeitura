import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kReleaseMode;
import 'package:flutter/material.dart';
import 'package:local_auth/local_auth.dart';
import 'package:uuid/uuid.dart';

import '../main.dart' show flutterLocalNotificationsPlugin, aoTocarLembrete, corPrimariaApp;
import 'app_theme.dart';
import 'ativacao_page.dart';
import 'banco_horas_page.dart';
import 'captura_face_page.dart';
import 'carteira_page.dart';
import 'comprovantes_page.dart';
import 'comunicados_page.dart';
import 'esqueci_bater_page.dart';
import 'erro_amigavel.dart';
import 'espelho_page.dart';
import 'face_liveness_service.dart';
import 'geo_service.dart';
import 'justificativa_page.dart';
import 'lembrete_manual_page.dart';
import 'lembrete_service.dart';
import 'minhas_ferias_page.dart';
import 'trilha_page.dart';
import 'gestao_page.dart';
import 'notificacoes_page.dart';
import 'privacidade_page.dart';
import 'satisfacao_page.dart';
import 'offline_queue.dart';
import 'ponto_api.dart';
import 'sessao_service.dart';
import 'sync_service.dart';
import 'voz_service.dart';

/// Tela principal: um único botão grande "Registrar ponto".
/// Acessível para idosos: fonte grande, alto contraste, toque generoso.
class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  // URL do backend. Em DEBUG, cai no localhost do emulador para conveniência de dev. Em RELEASE,
  // NÃO há fallback: exige --dart-define=API_BASE_URL=https://<backend> no build (senão fica vazio
  // e as chamadas falham de forma visível — nunca cleartext para localhost em produção).
  static const String _baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: kReleaseMode ? '' : 'http://10.0.2.2:8080',
  );

  final _sessaoService = SessaoService();
  final _queue = OfflineQueue();
  final _geo = GeoService();
  final _uuid = const Uuid();

  // _api e _sync são nulos até a sessão ser carregada (deviceToken desconhecido).
  PontoApi? _api;
  SyncService? _sync;
  late final LembreteService _lembreteService =
      LembreteService(flutterLocalNotificationsPlugin);

  Sessao? _sessao;
  bool _carregando = true;
  bool _ocupado = false;
  int _pendentes = 0;

  // Feedback ao usuário
  String? _mensagemSucesso;
  String? _mensagemErro;
  String? _mensagemAviso;

  /// Acessibilidade por voz (12.4.13): fala o status e ouve o comando de bater.
  final VozService _voz = VozService();

  /// Fala o status em voz alta (idosos/PCD). O leitor de tela também anuncia o texto exibido.
  ///
  /// Aparelho sem engine de TTS (AOSP/sem serviços Google) lança PlatformException: a fala é um
  /// reforço, então a falha é engolida de propósito — a mensagem já está na tela.
  void _falarStatus(String texto) {
    if (!mounted) return;
    unawaited(_voz.falar(texto).catchError((_) {}));
  }

  /// Política do órgão (de /api/me/config): exige verificação antes de bater?
  bool _verificacaoObrigatoria = false;

  /// O servidor é chefia de algum órgão (de /api/me/chefia)? Habilita a tela de Gestão.
  bool _souGestor = false;

  /// Identidade visual do ente (white-label).
  String? _nomeApp;
  String? _logoUrl;

  @override
  void initState() {
    super.initState();
    _carregarSessao();
    // Registrar a rota de bater ponto via notificação
    _registrarRotaBater();
  }

  /// Conecta o tap da notificação (via navigatorKey) ao fluxo de batida.
  void _registrarRotaBater() {
    // Tap do lembrete com o app aberto/em background → fluxo de batida.
    aoTocarLembrete = _iniciarBatidaPorNotificacao;
    // Se o app foi aberto a partir de uma notificação enquanto encerrado,
    // getNotificationAppLaunchDetails retorna o payload.
    flutterLocalNotificationsPlugin
        .getNotificationAppLaunchDetails()
        .then((details) {
      if (details?.didNotificationLaunchApp == true &&
          details?.notificationResponse?.payload == kPayloadBater) {
        // Aguarda o primeiro frame para garantir que o context esteja disponível.
        WidgetsBinding.instance.addPostFrameCallback((_) {
          _iniciarBatidaPorNotificacao();
        });
      }
    });
  }

  Future<void> _carregarSessao() async {
    final s = await _sessaoService.carregar();
    if (!mounted) return;
    if (s != null) {
      final api = PontoApi(baseUrl: _baseUrl, deviceToken: s.deviceToken);
      setState(() {
        _sessao = s;
        _api = api;
        _sync = SyncService(_queue, api);
        _carregando = false;
      });
      // Sobe o que ficou pendente e passa a sincronizar sozinho quando a conexão voltar —
      // a tela promete "será registrado ao reconectar", então não pode depender de toque manual.
      unawaited(_sincronizarEmSegundoPlano());
      _sync?.iniciarAutoSync(aoSincronizar: (_) => _atualizarPendentes());
      await _atualizarPendentes();
      await _agendarLembretes(api, s.vinculoId);
      try {
        final exige = await api.verificacaoObrigatoria();
        if (mounted) setState(() => _verificacaoObrigatoria = exige);
      } catch (_) {
        // sem config disponível → mantém o padrão (sem verificação)
      }
      try {
        final chefia = await api.chefiaResumo();
        if (mounted) setState(() => _souGestor = chefia.souGestor);
      } catch (_) {
        // sem dados de chefia → trata como não-gestor
      }
      try {
        final brand = await api.getBranding();
        if (mounted) {
          setState(() {
            _nomeApp = brand.nomeApp;
            _logoUrl = _resolverUrl(brand.logoUrl);
          });
          corPrimariaApp.value = _corDeHex(brand.corPrimaria);
        }
      } catch (_) {
        // sem branding → mantém a identidade padrão
      }
    } else {
      setState(() => _carregando = false);
    }
  }

  /// Converte "#RRGGBB" (ou "RRGGBB") em Color; volta ao azul gov.br se inválido.
  Color _corDeHex(String hex) {
    var h = hex.replaceAll('#', '').trim();
    if (h.length == 6) h = 'FF$h';
    return Color(int.tryParse(h, radix: 16) ?? 0xFF1351B4);
  }

  /// Resolve a URL do logo: absoluta (http) usa direto; relativa (logo no banco, ex.:
  /// "/api/publico/branding/{slug}/logo") é resolvida contra o baseUrl do app.
  String? _resolverUrl(String? url) {
    if (url == null || url.isEmpty) return null;
    if (url.startsWith('http')) return url;
    final base = _baseUrl.endsWith('/') ? _baseUrl.substring(0, _baseUrl.length - 1) : _baseUrl;
    return url.startsWith('/') ? '$base$url' : '$base/$url';
  }

  Future<void> _atualizarPendentes() async {
    final p = await _queue.pendentes();
    if (mounted) setState(() => _pendentes = p.length);
  }

  /// Busca a jornada vigente do vínculo e agenda os lembretes.
  /// Em caso de falha, degrada graciosamente (sem crash).
  Future<void> _agendarLembretes(PontoApi api, String vinculoId) async {
    try {
      // O backend resolve a jornada vigente do próprio servidor (deriva do token).
      final horarios = await api.jornada();
      if (horarios.isEmpty) return;
      await _lembreteService.agendarDaJornada(horarios);
    } catch (_) {
      // Falha silenciosa — lembretes não são críticos
    }
  }

  // ---------------------------------------------------------------------------
  // Fluxo de batida de ponto (compartilhado entre botão e tap de notificação)
  // ---------------------------------------------------------------------------

  /// Chamado quando o usuário toca na notificação enquanto o app está aberto.
  void _iniciarBatidaPorNotificacao() {
    if (!mounted) return;
    // Navega para a tela de captura facial diretamente (sem esperar o tap manual)
    _baterPontoOnline();
  }

  Future<void> _baterPontoOnline() async {
    final sessao = _sessao;
    final api = _api;
    if (sessao == null || api == null) return;

    setState(() {
      _ocupado = true;
      _mensagemSucesso = null;
      _mensagemErro = null;
      _mensagemAviso = null;
    });

    try {
      final posicao = await _geo.posicaoAtual();

      // Anti-fraude: recusa localização simulada (apps de GPS falso).
      if (posicao != null && posicao.isMocked) {
        if (!mounted) return;
        setState(() {
          _ocupado = false;
          _mensagemErro =
              'Localização simulada detectada. Desative apps de localização falsa para bater o ponto.';
        });
        return;
      }

      // Verificação conforme a política do órgão (sem verificação / aparelho / facial).
      final verificado = await _verificarParaBater();
      if (!verificado) {
        if (!mounted) return;
        setState(() {
          _ocupado = false;
          _mensagemErro = 'Verificação não concluída. O ponto não foi registrado.';
        });
        return;
      }

      final sync = _sync;
      final online = sync != null && await sync.online();

      if (online) {
        final resp = await api.bater(
          vinculoId: sessao.vinculoId,
          idempotencyKey: _uuid.v4(),
          latitude: posicao?.latitude,
          longitude: posicao?.longitude,
          offline: false,
          dataHoraDispositivo: DateTime.now().toUtc(),
        );
        await _atualizarPendentes();
        setState(() {
          _mensagemSucesso = resp.mensagem.isNotEmpty
              ? resp.mensagem
              : '${resp.rotuloTipo} registrado às ${resp.horaLocal}';
          // A localização é só verificação do administrador: o servidor não recebe alerta
          // de "fora da área" — a batida é confirmada normalmente.
          _mensagemAviso = null;
        });
        _falarStatus(_mensagemSucesso!);
      } else {
        final idempotencyKey = _uuid.v4();
        await _queue.enfileirar({
          '_intent': 'bater',
          'vinculoId': sessao.vinculoId,
          'idempotencyKey': idempotencyKey,
          'latitude': posicao?.latitude,
          'longitude': posicao?.longitude,
          'offline': true,
          'dataHoraDispositivo': DateTime.now().toUtc().toIso8601String(),
        });
        await _atualizarPendentes();
        setState(() => _mensagemSucesso =
            'Sem conexão: ponto salvo e será registrado ao reconectar.');
        _falarStatus(_mensagemSucesso!);
      }
    } catch (e) {
      // Conectividade "mentiu" (portal cativo / backend fora do ar): não perder a batida —
      // enfileira com o horário real, como no ramo offline.
      if (e is DioException && _falhaDeRede(e) && _sessao != null) {
        await _queue.enfileirar({
          '_intent': 'bater',
          'vinculoId': _sessao!.vinculoId,
          'idempotencyKey': _uuid.v4(),
          'offline': true,
          'dataHoraDispositivo': DateTime.now().toUtc().toIso8601String(),
        });
        await _atualizarPendentes();
        setState(() => _mensagemSucesso =
            'Sem conexão: ponto salvo e será registrado ao reconectar.');
        _falarStatus(_mensagemSucesso!);
      } else if (dispositivoRevogado(e)) {
        // RH desvinculou o aparelho: sem isto o usuário via erro técnico a cada tentativa.
        await _encerrarPorRevogacao();
      } else {
        setState(() => _mensagemErro = mensagemDeErro(e, padrao: 'Falha ao registrar ponto.'));
        _falarStatus('Falha ao registrar o ponto.');
      }
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  static bool _falhaDeRede(DioException e) =>
      e.type == DioExceptionType.connectionError ||
      e.type == DioExceptionType.connectionTimeout ||
      e.type == DioExceptionType.sendTimeout ||
      e.type == DioExceptionType.receiveTimeout;

  /// Sobe a fila pendente sem travar a tela.
  Future<void> _sincronizarEmSegundoPlano() async {
    try {
      final r = await _sync?.sincronizar();
      if (r != null && (r.enviados > 0 || r.rejeitados > 0)) await _atualizarPendentes();
    } catch (_) {
      // Sincronização é best-effort; a fila permanece para a próxima tentativa.
    }
  }

  /// Dispositivo revogado pelo RH: limpa a sessão e volta para a ativação com aviso claro.
  Future<void> _encerrarPorRevogacao() async {
    await _sessaoService.limpar();
    if (!mounted) return;
    setState(() {
      _sessao = null;
      _api = null;
      _mensagemSucesso = null;
      _mensagemErro = 'Este aparelho foi desvinculado. Solicite um novo código de ativação ao RH.';
    });
  }

  /// Verificação antes de bater, conforme a política do órgão:
  /// sem verificação → segue direto; com verificação → biometria/PIN/desenho do
  /// aparelho; se o aparelho não tiver bloqueio, usa a verificação facial do app.
  /// Acessibilidade (12.4.13): ouve "bater ponto" e registra por voz; fala o retorno.
  Future<void> _baterPorVoz() async {
    if (_ocupado) return;
    // A instrução falada é opcional: se o aparelho não tem TTS, mostra na tela e segue para o
    // reconhecimento — antes, a exceção do TTS abortava o fluxo inteiro sem nenhum feedback.
    try {
      await _voz.falar('Diga: bater ponto');
    } catch (_) {
      if (mounted) setState(() => _mensagemAviso = 'Diga: "bater ponto"');
    }
    try {
      final resultado = await _voz.ouvirComandoBater();
      if (!mounted) return;
      if (resultado == true) {
        await _baterPontoOnline();
      } else if (resultado == false) {
        _falarStatus('Não entendi. Toque no botão para registrar o ponto.');
      } else {
        _falarStatus('Não consegui ouvir. Verifique a permissão de microfone.');
      }
    } catch (e) {
      if (mounted) {
        setState(() => _mensagemErro =
            'Recurso de voz indisponível neste aparelho. Use o botão para registrar o ponto.');
      }
    }
  }

  Future<bool> _verificarParaBater() async {
    if (!_verificacaoObrigatoria) return true;
    final auth = LocalAuthentication();
    bool suportaAparelho = false;
    try {
      suportaAparelho = await auth.isDeviceSupported();
    } catch (_) {
      suportaAparelho = false;
    }
    if (suportaAparelho) {
      try {
        return await auth.authenticate(
          localizedReason: 'Confirme sua identidade para bater o ponto',
          options: const AuthenticationOptions(biometricOnly: false, stickyAuth: true),
        );
      } catch (_) {
        return false;
      }
    }
    return _verificarFacial();
  }

  /// Verificação facial (liveness) — usada quando o aparelho não tem bloqueio próprio.
  Future<bool> _verificarFacial() async {
    if (!mounted) return false;
    final caminho = await Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const CapturaFacePage()),
    );
    if (caminho == null) return false;
    final liveness = FaceLivenessService();
    final ok = await liveness.verificar(caminho);
    liveness.dispose();
    return ok;
  }

  // ---------------------------------------------------------------------------
  // Sessão / ativação
  // ---------------------------------------------------------------------------

  Future<void> _abrirAtivacao() async {
    final sessao = await Navigator.of(context).push<Sessao>(
      MaterialPageRoute(
        builder: (_) => AtivacaoPage(baseUrl: _baseUrl),
      ),
    );
    if (sessao != null && mounted) {
      final api = PontoApi(baseUrl: _baseUrl, deviceToken: sessao.deviceToken);
      setState(() {
        _sessao = sessao;
        _api = api;
        _sync = SyncService(_queue, api);
        _mensagemSucesso = null;
        _mensagemErro = null;
        _mensagemAviso = null;
      });
      await _atualizarPendentes();
      await _agendarLembretes(api, sessao.vinculoId);
    }
  }

  Future<void> _sairDoAparelho() async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Sair deste aparelho'),
        content: const Text(
          'Este dispositivo será desvinculado da sua matrícula.\n'
          'Para usar o app novamente, será necessário um novo código de ativação.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Confirmar saída'),
          ),
        ],
      ),
    );
    if (confirmar != true) return;
    await _sessaoService.limpar();
    await _lembreteService.cancelarTodos();
    if (mounted) {
      setState(() {
        _sessao = null;
        _api = null;
        _sync = null;
        _mensagemSucesso = null;
        _mensagemErro = null;
        _mensagemAviso = null;
      });
    }
  }

  // ---------------------------------------------------------------------------
  // Menu
  // ---------------------------------------------------------------------------

  void _abrirMenu() {
    final sessao = _sessao;
    final api = _api;
    showModalBottomSheet<void>(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 8),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey[400],
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 8),
            _MenuTile(
              icon: Icons.list_alt,
              label: 'Meus comprovantes',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => ComprovantesPage(
                          api: api,
                          vinculoId: sessao.vinculoId,
                        ),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.calendar_month,
              label: 'Meu espelho',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) =>
                            EspelhoPage(api: api, vinculoId: sessao.vinculoId),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.access_time_filled,
              label: 'Banco de horas',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => BancoHorasPage(
                          api: api,
                          vinculoId: sessao.vinculoId,
                        ),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.description_outlined,
              label: 'Solicitar justificativa',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => JustificativaPage(
                          api: api,
                          vinculoId: sessao.vinculoId,
                        ),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.more_time,
              label: 'Esqueci de bater',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => EsqueciBaterPage(api: api),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.notifications_outlined,
              label: 'Notificações',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => NotificacoesPage(
                          api: api,
                          servidorId: sessao.dispositivoId,
                        ),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.badge_outlined,
              label: 'Carteira funcional',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => CarteiraPage(api: api),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.history,
              label: 'Meu histórico',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => TrilhaPage(api: api),
                      ));
                    },
            ),
            if (_souGestor && api != null)
              _MenuTile(
                icon: Icons.supervisor_account_outlined,
                label: 'Gestão da equipe',
                onTap: () {
                  Navigator.pop(ctx);
                  Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => GestaoPage(api: api),
                  ));
                },
              ),
            _MenuTile(
              icon: Icons.beach_access,
              label: 'Minhas férias',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => MinhasFeriasPage(api: api),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.campaign_outlined,
              label: 'Comunicados',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => ComunicadosPage(api: api),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.star_outline,
              label: 'Avaliar',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => SatisfacaoPage(api: api),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.privacy_tip_outlined,
              label: 'Privacidade (LGPD)',
              onTap: (sessao == null || api == null)
                  ? null
                  : () {
                      Navigator.pop(ctx);
                      Navigator.of(context).push(MaterialPageRoute(
                        builder: (_) => PrivacidadePage(api: api),
                      ));
                    },
            ),
            _MenuTile(
              icon: Icons.alarm,
              label: 'Configurar lembrete manual',
              onTap: () {
                Navigator.pop(ctx);
                Navigator.of(context).push(MaterialPageRoute(
                  builder: (_) => LembreteManualPage(
                    lembreteService: _lembreteService,
                  ),
                ));
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Build
  // ---------------------------------------------------------------------------

  @override
  Widget build(BuildContext context) {
    if (_carregando) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    // Build de release sem --dart-define=API_BASE_URL: sem este aviso, toda chamada falharia com
    // "No host specified in URI" e o RH acharia que o código de ativação é que está errado.
    if (_baseUrl.isEmpty) {
      return Scaffold(
        appBar: AppBar(title: const Text('Ponto Municipal')),
        body: const Padding(
          padding: EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.error_outline, size: 72, color: Colors.redAccent),
              SizedBox(height: 24),
              Text(
                'Aplicativo instalado incorretamente',
                style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                textAlign: TextAlign.center,
              ),
              SizedBox(height: 12),
              Text(
                'O endereço do servidor não foi configurado nesta versão do aplicativo. '
                'Entre em contato com o suporte da prefeitura.',
                style: TextStyle(fontSize: 17),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      );
    }

    final theme = Theme.of(context);
    final pontoColors = theme.extension<PontoColors>();
    final sessao = _sessao;

    if (sessao == null) {
      // Ainda não ativado → tela de boas-vindas com botão para ativar
      return Scaffold(
        appBar: AppBar(title: const Text('Ponto Municipal')),
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(32),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Icon(Icons.badge_outlined,
                    size: 80, color: theme.colorScheme.primary),
                const SizedBox(height: 24),
                Text(
                  'Bem-vindo ao\nPonto Municipal',
                  style: theme.textTheme.headlineMedium
                      ?.copyWith(color: theme.colorScheme.primary),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 16),
                Text(
                  'Para começar, informe o código de ativação fornecido pelo RH.',
                  style: theme.textTheme.bodyLarge,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 48),
                ElevatedButton.icon(
                  onPressed: _abrirAtivacao,
                  icon: const Icon(Icons.login),
                  label: const Text('Ativar este aparelho'),
                  style: ElevatedButton.styleFrom(
                    minimumSize: const Size.fromHeight(72),
                    textStyle: const TextStyle(
                        fontSize: 22, fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(_nomeApp ?? 'Ponto Municipal'),
        leading: _logoUrl != null
            ? Padding(
                padding: const EdgeInsets.all(8),
                child: Image.network(_logoUrl!,
                    errorBuilder: (_, __, ___) => const SizedBox.shrink()),
              )
            : null,
        actions: [
          IconButton(
            icon: const Icon(Icons.menu),
            tooltip: 'Menu',
            onPressed: _abrirMenu,
          ),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Saudação
              if (sessao.nomeDispositivo.isNotEmpty) ...[
                Text(
                  'Olá, ${sessao.nomeDispositivo}',
                  style: theme.textTheme.titleLarge,
                ),
                const SizedBox(height: 4),
              ],
              Text(
                'Toque o botão abaixo para registrar seu ponto.',
                style: theme.textTheme.bodyLarge
                    ?.copyWith(color: Colors.grey[700]),
              ),
              const SizedBox(height: 32),

              // BOTÃO ÚNICO — grande, acessível
              Semantics(
                label: 'Registrar ponto',
                button: true,
                child: ElevatedButton.icon(
                  onPressed: _ocupado ? null : _baterPontoOnline,
                  style: ElevatedButton.styleFrom(
                    minimumSize: const Size.fromHeight(96),
                    textStyle: const TextStyle(
                        fontSize: 26, fontWeight: FontWeight.bold),
                    backgroundColor: theme.colorScheme.primary,
                    foregroundColor: theme.colorScheme.onPrimary,
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16)),
                  ),
                  icon: _ocupado
                      ? const SizedBox(
                          width: 28,
                          height: 28,
                          child: CircularProgressIndicator(
                              strokeWidth: 3, color: Colors.white),
                        )
                      : const Icon(Icons.fingerprint, size: 36),
                  label: const Text('Registrar ponto'),
                ),
              ),

              const SizedBox(height: 12),

              // Acessibilidade por voz (12.4.13): bater falando "bater ponto".
              Semantics(
                label: 'Bater ponto por voz',
                button: true,
                child: OutlinedButton.icon(
                  onPressed: _ocupado ? null : _baterPorVoz,
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(64),
                    textStyle: const TextStyle(
                        fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  icon: const Icon(Icons.mic, size: 30),
                  label: const Text('Bater por voz'),
                ),
              ),

              const SizedBox(height: 24),

              // Feedback de sucesso
              if (_mensagemSucesso != null)
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: (pontoColors?.sucesso ?? Colors.green)
                        .withAlpha(26),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                        color: pontoColors?.sucesso ?? Colors.green,
                        width: 1.5),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.check_circle,
                          color: pontoColors?.sucesso ?? Colors.green,
                          size: 28),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          _mensagemSucesso!,
                          style: TextStyle(
                            color: pontoColors?.sucesso ?? Colors.green,
                            fontSize: 20,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),

              // Aviso — ex.: batida fora da área da cerca do órgão
              if (_mensagemAviso != null)
                Container(
                  margin: const EdgeInsets.only(top: 12),
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF3CD),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: const Color(0xFFE0A800), width: 1.5),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.warning_amber_rounded,
                          color: Color(0xFF8A6D00), size: 28),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          _mensagemAviso!,
                          style: const TextStyle(
                            color: Color(0xFF6B5400),
                            fontSize: 18,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),

              // Feedback de erro
              if (_mensagemErro != null)
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color:
                        (pontoColors?.erro ?? theme.colorScheme.error).withAlpha(26),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                        color: pontoColors?.erro ?? theme.colorScheme.error,
                        width: 1.5),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.error_outline,
                          color: pontoColors?.erro ?? theme.colorScheme.error,
                          size: 28),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          _mensagemErro!,
                          style: TextStyle(
                            color: pontoColors?.erro ?? theme.colorScheme.error,
                            fontSize: 18,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),

              const Spacer(),

              // Pendentes e ações secundárias
              if (_pendentes > 0)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Row(
                    children: [
                      const Icon(Icons.cloud_upload_outlined,
                          size: 20, color: Colors.orange),
                      const SizedBox(width: 8),
                      Text(
                        '$_pendentes registro(s) aguardando sincronização',
                        style:
                            theme.textTheme.bodyMedium?.copyWith(color: Colors.orange[800]),
                      ),
                    ],
                  ),
                ),

              OutlinedButton.icon(
                onPressed: _ocupado
                    ? null
                    : () async {
                        final sync = _sync;
                        if (sync == null) return;
                        final r = await sync.sincronizar();
                        await _atualizarPendentes();
                        if (mounted) {
                          setState(() {
                            _mensagemAviso = null;
                            if (r.enviados > 0) {
                              _mensagemSucesso = '${r.enviados} registro(s) sincronizado(s).';
                              _mensagemErro = null;
                            } else if (r.rejeitados > 0) {
                              // Antes esses itens ficavam presos na fila para sempre, e a tela
                              // culpava a conexão — agora o motivo real aparece.
                              _mensagemErro = '${r.rejeitados} registro(s) recusado(s) pelo servidor. '
                                  'Procure o RH para regularizar.';
                              _mensagemSucesso = null;
                            } else if (r.semConexao) {
                              _mensagemErro = 'Sem conexão. Tente novamente quando houver internet.';
                              _mensagemSucesso = null;
                            } else {
                              _mensagemErro = 'Nenhum registro aguardando sincronização.';
                              _mensagemSucesso = null;
                            }
                          });
                        }
                      },
                icon: const Icon(Icons.sync),
                label: const Text('Sincronizar agora'),
              ),

              const SizedBox(height: 8),

              TextButton(
                onPressed: _sairDoAparelho,
                child: const Text('Sair deste aparelho'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Tile acessível para o menu inferior.
class _MenuTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback? onTap;

  const _MenuTile({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, size: 28),
      title: Text(label, style: const TextStyle(fontSize: 18)),
      minVerticalPadding: 14,
      enabled: onTap != null,
      onTap: onTap,
    );
  }
}
