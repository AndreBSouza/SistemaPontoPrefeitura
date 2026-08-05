import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Sessão do dispositivo ativado: guardada em armazenamento seguro.
class Sessao {
  final String deviceToken;
  final String tenantId;
  final String vinculoId;
  final String dispositivoId;
  final String nomeDispositivo;

  const Sessao({
    required this.deviceToken,
    required this.tenantId,
    required this.vinculoId,
    required this.dispositivoId,
    required this.nomeDispositivo,
  });
}

/// Persiste e lê a sessão do dispositivo usando flutter_secure_storage.
///
/// Após ativação, o app autentica cada requisição via X-Device-Token.
/// Não é necessário novo login no mesmo aparelho.
class SessaoService {
  static const _storage = FlutterSecureStorage();

  static const _kDeviceToken = 'sessao_device_token';
  static const _kTenantId = 'sessao_tenant_id';
  static const _kVinculoId = 'sessao_vinculo_id';
  static const _kDispositivoId = 'sessao_dispositivo_id';
  static const _kNomeDispositivo = 'sessao_nome_dispositivo';

  Future<Sessao?> carregar() async {
    final token = await _storage.read(key: _kDeviceToken);
    if (token == null || token.isEmpty) return null;
    return Sessao(
      deviceToken: token,
      tenantId: await _storage.read(key: _kTenantId) ?? '',
      vinculoId: await _storage.read(key: _kVinculoId) ?? '',
      dispositivoId: await _storage.read(key: _kDispositivoId) ?? '',
      nomeDispositivo: await _storage.read(key: _kNomeDispositivo) ?? '',
    );
  }

  Future<void> salvar(Sessao s) async {
    await Future.wait([
      _storage.write(key: _kDeviceToken, value: s.deviceToken),
      _storage.write(key: _kTenantId, value: s.tenantId),
      _storage.write(key: _kVinculoId, value: s.vinculoId),
      _storage.write(key: _kDispositivoId, value: s.dispositivoId),
      _storage.write(key: _kNomeDispositivo, value: s.nomeDispositivo),
    ]);
  }

  Future<void> limpar() async {
    await Future.wait([
      _storage.delete(key: _kDeviceToken),
      _storage.delete(key: _kTenantId),
      _storage.delete(key: _kVinculoId),
      _storage.delete(key: _kDispositivoId),
      _storage.delete(key: _kNomeDispositivo),
    ]);
  }
}
