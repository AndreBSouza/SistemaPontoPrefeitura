import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

const _tipoLabel = <String, String>{
  'FERIAS': 'Férias',
  'LICENCA_MEDICA': 'Licença médica',
  'LICENCA_MATERNIDADE': 'Licença maternidade',
  'LICENCA_PATERNIDADE': 'Licença paternidade',
  'LICENCA_PREMIO': 'Licença-prêmio',
  'LICENCA_NOJO': 'Licença nojo',
  'OUTRA': 'Outra',
};

/// Tela "Minhas férias" — o servidor vê suas ausências programadas (férias/licenças).
class MinhasFeriasPage extends StatefulWidget {
  final PontoApi api;

  const MinhasFeriasPage({super.key, required this.api});

  @override
  State<MinhasFeriasPage> createState() => _MinhasFeriasPageState();
}

class _MinhasFeriasPageState extends State<MinhasFeriasPage> {
  List<AusenciaItem>? _itens;
  String? _erro;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    setState(() {
      _itens = null;
      _erro = null;
    });
    try {
      final lista = await widget.api.minhasAusencias();
      if (mounted) setState(() => _itens = lista);
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível carregar suas férias e licenças.'));
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Minhas férias'),
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
            : _itens == null
                ? const Center(child: CircularProgressIndicator())
                : _itens!.isEmpty
                    ? Center(
                        child: Text('Nenhuma férias ou licença programada.',
                            style: theme.textTheme.bodyLarge,
                            textAlign: TextAlign.center),
                      )
                    : RefreshIndicator(
                        onRefresh: _carregar,
                        child: ListView.separated(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 16, vertical: 12),
                          itemCount: _itens!.length,
                          separatorBuilder: (_, __) => const Divider(height: 1),
                          itemBuilder: (ctx, i) {
                            final a = _itens![i];
                            return ListTile(
                              contentPadding: const EdgeInsets.symmetric(
                                  horizontal: 8, vertical: 10),
                              leading: const Icon(Icons.beach_access, size: 28),
                              title: Text(_tipoLabel[a.tipo] ?? a.tipo,
                                  style: theme.textTheme.titleMedium),
                              subtitle: Text(
                                  '${a.dataInicio} a ${a.dataFim}  ·  ${a.dias} dia(s)',
                                  style: theme.textTheme.bodyMedium),
                            );
                          },
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
