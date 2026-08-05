import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Comunicados" — comunicados oficiais (broadcast) da prefeitura ao servidor.
class ComunicadosPage extends StatefulWidget {
  final PontoApi api;

  const ComunicadosPage({super.key, required this.api});

  @override
  State<ComunicadosPage> createState() => _ComunicadosPageState();
}

class _ComunicadosPageState extends State<ComunicadosPage> {
  List<ComunicadoItem>? _itens;
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
      final lista = await widget.api.listarComunicados();
      if (mounted) setState(() => _itens = lista);
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível carregar os comunicados.'));
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Comunicados'),
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
                          'Nenhum comunicado.',
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
                                  horizontal: 8, vertical: 10),
                              leading: const Icon(Icons.campaign_outlined,
                                  size: 28),
                              title: Text(item.titulo,
                                  style: theme.textTheme.titleMedium),
                              subtitle: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const SizedBox(height: 4),
                                  Text(item.mensagem,
                                      style: theme.textTheme.bodyMedium),
                                  const SizedBox(height: 4),
                                  Text(
                                    item.publicadoEm.length >= 10
                                        ? item.publicadoEm.substring(0, 10)
                                        : item.publicadoEm,
                                    style: theme.textTheme.bodySmall
                                        ?.copyWith(color: Colors.grey[600]),
                                  ),
                                ],
                              ),
                              isThreeLine: true,
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
