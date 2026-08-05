import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Meus comprovantes" — lista NSR + rótulo + hora de cada batida.
class ComprovantesPage extends StatefulWidget {
  final PontoApi api;
  final String vinculoId;

  const ComprovantesPage({
    super.key,
    required this.api,
    required this.vinculoId,
  });

  @override
  State<ComprovantesPage> createState() => _ComprovantesPageState();
}

class _ComprovantesPageState extends State<ComprovantesPage> {
  List<RegistroItem>? _itens;
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
      final lista = await widget.api.listarRegistros(widget.vinculoId);
      if (mounted) setState(() => _itens = lista);
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível carregar os comprovantes.'));
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Meus comprovantes'),
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
                        child: Text(
                          'Nenhum comprovante encontrado.',
                          style: theme.textTheme.bodyLarge,
                          textAlign: TextAlign.center,
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: _carregar,
                        child: ListView.separated(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 16, vertical: 12),
                          itemCount: _itens!.length,
                          separatorBuilder: (_, __) => const Divider(height: 1),
                          itemBuilder: (ctx, i) {
                            final item = _itens![i];
                            return ListTile(
                              contentPadding: const EdgeInsets.symmetric(
                                  horizontal: 8, vertical: 8),
                              leading: CircleAvatar(
                                backgroundColor:
                                    theme.colorScheme.primaryContainer,
                                child: Text(
                                  item.horaLocal.isNotEmpty
                                      ? item.horaLocal
                                      : '--',
                                  style: TextStyle(
                                    fontSize: 12,
                                    fontWeight: FontWeight.bold,
                                    color:
                                        theme.colorScheme.onPrimaryContainer,
                                  ),
                                ),
                              ),
                              title: Text(
                                item.rotuloTipo.isNotEmpty
                                    ? item.rotuloTipo
                                    : item.tipo,
                                style: theme.textTheme.titleMedium,
                              ),
                              subtitle: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('NSR ${item.nsr}', style: theme.textTheme.bodyMedium),
                                  // Código hash exigido no comprovante do REP-P (art. 79, VIII).
                                  // É o mesmo valor que consta no AFD entregue à fiscalização,
                                  // então serve ao servidor como prova da marcação.
                                  if (item.codigoHash.isNotEmpty)
                                    Padding(
                                      padding: const EdgeInsets.only(top: 4),
                                      child: Text(
                                        'Código: ${item.codigoHash.substring(0, 16)}…',
                                        style: theme.textTheme.bodySmall?.copyWith(
                                          fontFamily: 'monospace',
                                          color: theme.colorScheme.onSurfaceVariant,
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                              isThreeLine: item.codigoHash.isNotEmpty,
                              trailing: Text(
                                item.dataLocal, // data no fuso do município (não o Instant UTC)
                                style: theme.textTheme.bodySmall,
                              ),
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
