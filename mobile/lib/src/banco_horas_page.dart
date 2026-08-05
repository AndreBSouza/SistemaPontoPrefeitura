import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Saldo de banco de horas" — saldo em destaque, números grandes.
class BancoHorasPage extends StatefulWidget {
  final PontoApi api;
  final String vinculoId;

  const BancoHorasPage({
    super.key,
    required this.api,
    required this.vinculoId,
  });

  @override
  State<BancoHorasPage> createState() => _BancoHorasPageState();
}

class _BancoHorasPageState extends State<BancoHorasPage> {
  ResumoMe? _resumo;
  String? _erro;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    setState(() {
      _resumo = null;
      _erro = null;
    });
    try {
      final r = await widget.api.resumo();
      if (mounted) setState(() => _resumo = r);
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível carregar o saldo.'));
    }
  }

  String _formatarSaldo(int minutos) {
    final positivo = minutos >= 0;
    final abs = minutos.abs();
    final h = abs ~/ 60;
    final m = abs % 60;
    final sinal = positivo ? '+' : '-';
    return '$sinal${h}h ${m.toString().padLeft(2, '0')}min';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final positivo = (_resumo?.saldoBancoHorasMinutos ?? 0) >= 0;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Banco de horas'),
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
            ? _ErroRecarregar(mensagem: _erro!, onRecarregar: _carregar)
            : _resumo == null
                ? const Center(child: CircularProgressIndicator())
                : Center(
                    child: Padding(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            positivo
                                ? Icons.trending_up
                                : Icons.trending_down,
                            size: 72,
                            color: positivo
                                ? Colors.green[700]
                                : theme.colorScheme.error,
                          ),
                          const SizedBox(height: 24),
                          Text(
                            'Seu saldo',
                            style: theme.textTheme.titleLarge,
                          ),
                          const SizedBox(height: 8),
                          Text(
                            _formatarSaldo(_resumo!.saldoBancoHorasMinutos),
                            style: theme.textTheme.displaySmall?.copyWith(
                              color: positivo
                                  ? Colors.green[700]
                                  : theme.colorScheme.error,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            '(${_resumo!.saldoBancoHorasMinutos} minutos)',
                            style: theme.textTheme.bodyMedium
                                ?.copyWith(color: Colors.grey[600]),
                          ),
                          const SizedBox(height: 32),
                          Card(
                            color: Colors.green[50],
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 24, vertical: 16),
                              child: Column(
                                children: [
                                  Text('Horas extras nesta semana',
                                      style: theme.textTheme.titleMedium),
                                  const SizedBox(height: 6),
                                  Text(
                                    _formatarSaldo(
                                        _resumo!.horaExtraSemanaMinutos),
                                    style:
                                        theme.textTheme.headlineSmall?.copyWith(
                                      color: Colors.green[800],
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ],
                              ),
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

class _ErroRecarregar extends StatelessWidget {
  final String mensagem;
  final VoidCallback onRecarregar;

  const _ErroRecarregar({required this.mensagem, required this.onRecarregar});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.error_outline,
                size: 48, color: theme.colorScheme.error),
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
