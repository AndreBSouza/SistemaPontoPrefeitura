import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'package:flutter/services.dart';

import 'ponto_api.dart';
import 'sessao_service.dart';

/// Tela de ativação do dispositivo.
///
/// O servidor informa o código gerado pelo RH (formato XXXX-9999).
/// O app troca esse código por um deviceToken persistido em armazenamento
/// seguro — única vez por aparelho.
class AtivacaoPage extends StatefulWidget {
  final String baseUrl;

  const AtivacaoPage({super.key, required this.baseUrl});

  @override
  State<AtivacaoPage> createState() => _AtivacaoPageState();
}

class _AtivacaoPageState extends State<AtivacaoPage> {
  final _formKey = GlobalKey<FormState>();
  final _codigoCtrl = TextEditingController();
  final _nomeCtrl = TextEditingController();
  bool _ocupado = false;
  String? _erro;

  @override
  void dispose() {
    _codigoCtrl.dispose();
    _nomeCtrl.dispose();
    super.dispose();
  }

  String? _validarCodigo(String? v) {
    if (v == null || v.trim().isEmpty) return 'Campo obrigatório';
    // Aceita XXXX-9999 ou apenas os 8 caracteres sem hífen
    final limpo = v.trim().replaceAll('-', '').toUpperCase();
    if (limpo.length < 4) return 'Código muito curto';
    return null;
  }

  Future<void> _ativar() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() {
      _ocupado = true;
      _erro = null;
    });
    try {
      // Instância pré-auth: sem token
      final api = PontoApi(baseUrl: widget.baseUrl);
      final result = await api.ativar(
        codigo: _codigoCtrl.text.trim().toUpperCase(),
        nomeDispositivo: _nomeCtrl.text.trim().isEmpty
            ? null
            : _nomeCtrl.text.trim(),
      );
      final sessao = Sessao(
        deviceToken: result.deviceToken,
        tenantId: result.tenantId,
        vinculoId: result.vinculoId,
        dispositivoId: result.dispositivoId,
        nomeDispositivo: result.nomeDispositivo,
      );
      await SessaoService().salvar(sessao);
      if (!mounted) return;
      Navigator.of(context).pop(sessao);
    } on Exception catch (e) {
      setState(() => _erro = _mensagemErro(e));
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  /// O backend responde 400 (código inválido), 409 (expirado/já usado) e 429 (rate limit) com
  /// `{"erro": "..."}` — antes o app procurava 404/401, que nunca chegam, e mostrava a exceção crua.
  String _mensagemErro(Exception e) =>
      mensagemDeErro(e, padrao: 'Não foi possível ativar. Confira o código e tente de novo.');

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Ativar dispositivo')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Icon(Icons.smartphone_outlined,
                    size: 72, color: theme.colorScheme.primary),
                const SizedBox(height: 16),
                Text(
                  'Bem-vindo ao Ponto Municipal',
                  style: theme.textTheme.headlineSmall,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                Text(
                  'Informe o código de ativação fornecido pelo RH.\n'
                  'Este aparelho ficará vinculado à sua matrícula.',
                  style: theme.textTheme.bodyLarge,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 32),
                TextFormField(
                  controller: _codigoCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Código de ativação *',
                    prefixIcon: Icon(Icons.vpn_key_outlined),
                    helperText: 'Exemplo: ABCD-2345',
                  ),
                  keyboardType: TextInputType.visiblePassword,
                  textCapitalization: TextCapitalization.characters,
                  autocorrect: false,
                  inputFormatters: [
                    _CodigoFormatter(),
                  ],
                  validator: _validarCodigo,
                  onChanged: (_) {
                    if (_erro != null) setState(() => _erro = null);
                  },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _nomeCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Nome deste aparelho (opcional)',
                    prefixIcon: Icon(Icons.devices_outlined),
                    helperText: 'Ex.: Celular do trabalho',
                  ),
                  textCapitalization: TextCapitalization.sentences,
                  autocorrect: false,
                ),
                const SizedBox(height: 12),
                Text(
                  'Ao ativar, você autoriza o uso de dados biométricos faciais '
                  'para verificação de presença, conforme a LGPD.',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: Colors.grey[700]),
                ),
                const SizedBox(height: 32),
                if (_erro != null) ...[
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.errorContainer,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.error_outline,
                            color: theme.colorScheme.error),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            _erro!,
                            style: TextStyle(
                                color: theme.colorScheme.error, fontSize: 16),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                ElevatedButton.icon(
                  onPressed: _ocupado ? null : _ativar,
                  icon: _ocupado
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.check_circle_outline),
                  label: const Text('Ativar dispositivo'),
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
      ),
    );
  }
}

/// Formata automaticamente o código como XXXX-YYYY enquanto o usuário digita.
class _CodigoFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
      TextEditingValue oldValue, TextEditingValue newValue) {
    final text = newValue.text.toUpperCase().replaceAll('-', '');
    final buffer = StringBuffer();
    for (int i = 0; i < text.length && i < 8; i++) {
      if (i == 4) buffer.write('-');
      buffer.write(text[i]);
    }
    final formatted = buffer.toString();
    return newValue.copyWith(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}
