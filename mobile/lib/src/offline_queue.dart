import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Fila local de registros pendentes (offline-first), persistida de forma
/// **cifrada** via flutter_secure_storage (Keystore/Keychain do sistema) — os
/// pontos (com geolocalização) não ficam mais em arquivo de texto puro.
///
/// Cada item pode ser do tipo "bater" (botão único, sem tipo) ou do tipo legado
/// "registrar" (com tipo explícito). O campo `_intent` diferencia os dois:
/// - `_intent == 'bater'`     → enviado para POST /api/me/bater
/// - `_intent == 'registrar'` (ou ausente) → enviado para POST /api/registros
class OfflineQueue {
  static const FlutterSecureStorage _storage = FlutterSecureStorage();
  static const String _chave = 'fila_registros_pendentes';

  Future<List<Map<String, dynamic>>> pendentes() async {
    final txt = await _storage.read(key: _chave);
    if (txt == null || txt.trim().isEmpty) return [];
    final lista = jsonDecode(txt) as List;
    return lista.map((e) => (e as Map).cast<String, dynamic>()).toList();
  }

  Future<void> enfileirar(Map<String, dynamic> registro) async {
    final atuais = await pendentes();
    atuais.add(registro);
    await _salvar(atuais);
  }

  Future<void> remover(String idempotencyKey) async {
    final atuais = await pendentes();
    atuais.removeWhere((r) => r['idempotencyKey'] == idempotencyKey);
    await _salvar(atuais);
  }

  Future<void> _salvar(List<Map<String, dynamic>> lista) async {
    await _storage.write(key: _chave, value: jsonEncode(lista));
  }
}
