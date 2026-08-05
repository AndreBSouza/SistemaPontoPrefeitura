import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Meu histórico" (trilha pessoal, 12.1.2): mostra, do mais recente ao mais antigo,
/// o que aconteceu com os registros do servidor (correções e justificativas) e a decisão.
class TrilhaPage extends StatefulWidget {
  final PontoApi api;

  const TrilhaPage({super.key, required this.api});

  @override
  State<TrilhaPage> createState() => _TrilhaPageState();
}

class _TrilhaPageState extends State<TrilhaPage> {
  List<TrilhaEventoItem>? _eventos;
  String? _erro;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    setState(() {
      _eventos = null;
      _erro = null;
    });
    try {
      final e = await widget.api.listarTrilha();
      if (mounted) setState(() => _eventos = e);
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível carregar o histórico.'));
    }
  }

  IconData _icone(String categoria) {
    switch (categoria) {
      case 'CORRECAO':
        return Icons.edit_calendar_outlined;
      case 'JUSTIFICATIVA':
        return Icons.description_outlined;
      default:
        return Icons.history;
    }
  }

  String _quando(String iso) {
    final d = DateTime.tryParse(iso)?.toLocal();
    if (d == null) return iso;
    String dd(int n) => n.toString().padLeft(2, '0');
    return '${dd(d.day)}/${dd(d.month)}/${d.year} ${dd(d.hour)}:${dd(d.minute)}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Meu histórico')),
      body: RefreshIndicator(
        onRefresh: _carregar,
        child: _erro != null
            ? ListView(children: [Padding(padding: const EdgeInsets.all(24), child: Text(_erro!))])
            : _eventos == null
                ? const Center(child: Padding(padding: EdgeInsets.all(32), child: CircularProgressIndicator()))
                : _eventos!.isEmpty
                    ? ListView(children: const [
                        Padding(
                          padding: EdgeInsets.all(32),
                          child: Text(
                            'Nenhum evento ainda. Aqui aparecem suas correções de marcação e '
                            'justificativas, com a decisão da chefia.',
                            textAlign: TextAlign.center,
                          ),
                        )
                      ])
                    : ListView.separated(
                        padding: const EdgeInsets.all(12),
                        itemCount: _eventos!.length,
                        separatorBuilder: (_, __) => const Divider(height: 1),
                        itemBuilder: (_, i) {
                          final e = _eventos![i];
                          return ListTile(
                            leading: Icon(_icone(e.categoria)),
                            title: Text(e.titulo),
                            subtitle: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                if (e.detalhe.isNotEmpty) Text(e.detalhe),
                                Text(_quando(e.instante),
                                    style: Theme.of(context).textTheme.bodySmall),
                              ],
                            ),
                          );
                        },
                      ),
      ),
    );
  }
}
