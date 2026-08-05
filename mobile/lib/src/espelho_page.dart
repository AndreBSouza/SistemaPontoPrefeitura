import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Meu espelho" — resumo mensal de frequência com números grandes.
class EspelhoPage extends StatefulWidget {
  final PontoApi api;
  final String vinculoId;

  const EspelhoPage({
    super.key,
    required this.api,
    required this.vinculoId,
  });

  @override
  State<EspelhoPage> createState() => _EspelhoPageState();
}

class _EspelhoPageState extends State<EspelhoPage> {
  EspelhoResponse? _espelho;
  String? _erro;
  // Competência padrão = mês atual
  late String _competencia;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _competencia =
        '${now.year}-${now.month.toString().padLeft(2, '0')}';
    _carregar();
  }

  Future<void> _carregar() async {
    setState(() {
      _espelho = null;
      _erro = null;
    });
    try {
      final e = await widget.api.espelho(
        vinculoId: widget.vinculoId,
        competencia: _competencia,
      );
      if (mounted) setState(() => _espelho = e);
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível carregar o espelho de ponto.'));
    }
  }

  String _formatarHoras(int minutos) {
    final h = minutos ~/ 60;
    final m = minutos % 60;
    return '${h}h ${m.toString().padLeft(2, '0')}min';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Meu espelho'),
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
            : _espelho == null
                ? const Center(child: CircularProgressIndicator())
                : SingleChildScrollView(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // Competência e status
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Competência: ${_espelho!.competencia}',
                              style: theme.textTheme.titleMedium,
                            ),
                            Chip(
                              label: Text(
                                _espelho!.status,
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 20),
                        // Totais em destaque
                        _CardTotal(
                          titulo: 'Horas trabalhadas',
                          valor: _formatarHoras(
                              _espelho!.totalMinutosTrabalhados),
                          cor: theme.colorScheme.primary,
                        ),
                        const SizedBox(height: 12),
                        _CardTotal(
                          titulo: 'Horas esperadas',
                          valor: _formatarHoras(
                              _espelho!.totalMinutosEsperados),
                          cor: Colors.grey[700]!,
                        ),
                        const SizedBox(height: 24),
                        if (_espelho!.dias.isNotEmpty) ...[
                          Text('Detalhamento por dia',
                              style: theme.textTheme.titleMedium),
                          const SizedBox(height: 8),
                          ...(_espelho!.dias.map((d) => _DiaTile(dia: d))),
                        ],
                      ],
                    ),
                  ),
      ),
    );
  }
}

class _CardTotal extends StatelessWidget {
  final String titulo;
  final String valor;
  final Color cor;

  const _CardTotal({
    required this.titulo,
    required this.valor,
    required this.cor,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: cor, width: 1.5),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(titulo,
                style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey[700],
                    fontWeight: FontWeight.w500)),
            const SizedBox(height: 4),
            Text(valor,
                style: TextStyle(
                    fontSize: 32,
                    fontWeight: FontWeight.bold,
                    color: cor)),
          ],
        ),
      ),
    );
  }
}

class _DiaTile extends StatelessWidget {
  final EspelhoDia dia;

  const _DiaTile({required this.dia});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final h = dia.minutosTrabalhados ~/ 60;
    final m = dia.minutosTrabalhados % 60;
    return ListTile(
      contentPadding: EdgeInsets.zero,
      title: Text(dia.data, style: theme.textTheme.bodyLarge),
      subtitle: dia.justificado
          ? const Text('Justificado')
          : null,
      trailing: dia.diaUtil
          ? Text(
              '${h}h ${m.toString().padLeft(2, '0')}min',
              style: theme.textTheme.bodyLarge
                  ?.copyWith(fontWeight: FontWeight.bold),
            )
          : Text('Folga',
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: Colors.grey)),
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
