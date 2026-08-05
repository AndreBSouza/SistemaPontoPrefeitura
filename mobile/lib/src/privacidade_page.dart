import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Privacidade (LGPD)": o titular vê os próprios dados (direito de acesso)
/// e gerencia o consentimento de uso de biometria. A exclusão/anonimização é
/// solicitada ao RH (CPF e registros são retidos por obrigação legal).
class PrivacidadePage extends StatefulWidget {
  final PontoApi api;

  const PrivacidadePage({super.key, required this.api});

  @override
  State<PrivacidadePage> createState() => _PrivacidadePageState();
}

class _PrivacidadePageState extends State<PrivacidadePage> {
  DadosTitular? _dados;
  bool _consentimentoBiometria = false;
  bool _salvando = false;
  String? _erro;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    setState(() {
      _dados = null;
      _erro = null;
    });
    try {
      final dados = await widget.api.meusDadosLgpd();
      final consent = await widget.api.consultarConsentimento('BIOMETRIA');
      if (mounted) {
        setState(() {
          _dados = dados;
          _consentimentoBiometria = consent;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível carregar seus dados.'));
    }
  }

  Future<void> _alternarConsentimento(bool valor) async {
    setState(() => _salvando = true);
    try {
      final novo = await widget.api.definirConsentimento('BIOMETRIA', valor);
      if (mounted) setState(() => _consentimentoBiometria = novo);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Não foi possível salvar: $e')));
      }
    } finally {
      if (mounted) setState(() => _salvando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Privacidade (LGPD)'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Atualizar',
            onPressed: _carregar,
          ),
        ],
      ),
      body: SafeArea(
        child: _erro != null
            ? _ErroLgpd(mensagem: _erro!, onRecarregar: _carregar)
            : _dados == null
                ? const Center(child: CircularProgressIndicator())
                : ListView(
                    padding: const EdgeInsets.all(20),
                    children: [
                      Text('Meus dados', style: theme.textTheme.titleLarge),
                      const SizedBox(height: 12),
                      _LinhaDado(rotulo: 'Nome', valor: _dados!.nome),
                      _LinhaDado(rotulo: 'CPF', valor: _dados!.cpf),
                      _LinhaDado(rotulo: 'E-mail', valor: _dados!.email ?? '—'),
                      _LinhaDado(rotulo: 'Vínculos', valor: '${_dados!.totalVinculos}'),
                      _LinhaDado(
                          rotulo: 'Registros de ponto',
                          valor: '${_dados!.totalRegistros}'),
                      const Divider(height: 40),
                      Text('Consentimentos', style: theme.textTheme.titleLarge),
                      const SizedBox(height: 8),
                      SwitchListTile(
                        title: const Text('Uso de biometria facial'),
                        subtitle: const Text(
                            'Permitir a verificação facial na batida do ponto'),
                        value: _consentimentoBiometria,
                        onChanged: _salvando ? null : _alternarConsentimento,
                      ),
                      const SizedBox(height: 24),
                      Text(
                        'Para excluir ou anonimizar seus dados, solicite ao RH do seu órgão. '
                        'O CPF e os registros de ponto são retidos por obrigação legal.',
                        style: theme.textTheme.bodySmall
                            ?.copyWith(color: Colors.grey[600]),
                      ),
                    ],
                  ),
      ),
    );
  }
}

class _LinhaDado extends StatelessWidget {
  final String rotulo;
  final String valor;

  const _LinhaDado({required this.rotulo, required this.valor});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 150,
            child: Text(rotulo,
                style: theme.textTheme.bodyLarge?.copyWith(color: Colors.grey[700])),
          ),
          Expanded(
            child: Text(valor,
                style: theme.textTheme.bodyLarge
                    ?.copyWith(fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }
}

class _ErroLgpd extends StatelessWidget {
  final String mensagem;
  final VoidCallback onRecarregar;

  const _ErroLgpd({required this.mensagem, required this.onRecarregar});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline, size: 48, color: theme.colorScheme.error),
            const SizedBox(height: 16),
            Text(mensagem,
                style: theme.textTheme.bodyLarge, textAlign: TextAlign.center),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: onRecarregar,
              icon: const Icon(Icons.refresh),
              label: const Text('Tentar novamente'),
            ),
          ],
        ),
      ),
    );
  }
}
