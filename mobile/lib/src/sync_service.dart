import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';

import 'offline_queue.dart';
import 'ponto_api.dart';

/// Resultado de uma sincronizacao, para a tela distinguir "sem conexao" de "rejeitado".
class ResultadoSync {
  final int enviados;
  final int rejeitados; // descartados da fila por erro definitivo do servidor (4xx)
  final bool semConexao;

  const ResultadoSync({
    this.enviados = 0,
    this.rejeitados = 0,
    this.semConexao = false,
  });
}

/// Sincroniza a fila offline com o backend quando ha conectividade (idempotente).
///
/// Suporta dois intents:
/// - `_intent == 'bater'`     → POST /api/registros/bater (botao unico, sem tipo)
/// - `_intent == 'registrar'` (ou ausente) → POST /api/registros (legado com tipo)
class SyncService {
  final OfflineQueue queue;
  final PontoApi api;

  StreamSubscription<List<ConnectivityResult>>? _assinatura;

  SyncService(this.queue, this.api);

  Future<bool> online() async {
    final resultado = await Connectivity().checkConnectivity();
    return !resultado.contains(ConnectivityResult.none);
  }

  /// Passa a sincronizar sozinho quando a conexao volta.
  ///
  /// A tela promete "sera registrado ao reconectar" — sem isto o registro so subia se o servidor
  /// lembrasse de tocar "Sincronizar agora", e a batida virava falta.
  void iniciarAutoSync({void Function(ResultadoSync)? aoSincronizar}) {
    _assinatura ??= Connectivity().onConnectivityChanged.listen((estado) async {
      if (estado.contains(ConnectivityResult.none)) return;
      final r = await sincronizar();
      if (r.enviados > 0 || r.rejeitados > 0) aoSincronizar?.call(r);
    });
  }

  void dispose() {
    _assinatura?.cancel();
    _assinatura = null;
  }

  /// Envia os pendentes em ordem FIFO.
  Future<ResultadoSync> sincronizar() async {
    if (!await online()) return const ResultadoSync(semConexao: true);
    final pendentes = await queue.pendentes();
    int enviados = 0;
    int rejeitados = 0;
    for (final r in pendentes) {
      try {
        final intent = r['_intent'] as String? ?? 'registrar';
        if (intent == 'bater') {
          await api.bater(
            vinculoId: r['vinculoId'] as String,
            idempotencyKey: r['idempotencyKey'] as String,
            latitude: (r['latitude'] as num?)?.toDouble(),
            longitude: (r['longitude'] as num?)?.toDouble(),
            offline: r['offline'] as bool? ?? true,
            dataHoraDispositivo: r['dataHoraDispositivo'] != null
                ? DateTime.parse(r['dataHoraDispositivo'] as String)
                : null,
          );
        } else {
          await api.registrar(
            vinculoId: r['vinculoId'] as String,
            tipo: r['tipo'] as String,
            idempotencyKey: r['idempotencyKey'] as String,
            latitude: (r['latitude'] as num?)?.toDouble(),
            longitude: (r['longitude'] as num?)?.toDouble(),
            offline: r['offline'] as bool? ?? false,
          );
        }
        await queue.remover(r['idempotencyKey'] as String);
        enviados++;
      } catch (e) {
        if (_rejeitadoDefinitivamente(e)) {
          // Erro que nao melhora com nova tentativa (payload invalido, vinculo desativado):
          // manter na fila faria o contador crescer para sempre sem nunca sincronizar.
          await queue.remover(r['idempotencyKey'] as String);
          rejeitados++;
        }
        // Erro de rede/5xx: mantem na fila para nova tentativa.
      }
    }
    return ResultadoSync(enviados: enviados, rejeitados: rejeitados);
  }

  /// 4xx (exceto 408/429, que sao temporarios) = o servidor nao vai aceitar numa retentativa.
  /// 401 fica de fora: e dispositivo revogado, tratado a parte (o usuario precisa reativar).
  static bool _rejeitadoDefinitivamente(Object e) {
    if (e is! DioException) return false;
    final status = e.response?.statusCode;
    if (status == null) return false;
    if (status == 401 || status == 408 || status == 429) return false;
    return status >= 400 && status < 500;
  }
}
