import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Carteira funcional" — identifica o servidor com os dados do vínculo e do ente.
class CarteiraPage extends StatefulWidget {
  final PontoApi api;

  const CarteiraPage({super.key, required this.api});

  @override
  State<CarteiraPage> createState() => _CarteiraPageState();
}

class _CarteiraPageState extends State<CarteiraPage> {
  CarteiraDigital? _carteira;
  String? _erro;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    setState(() {
      _carteira = null;
      _erro = null;
    });
    try {
      final c = await widget.api.minhaCarteira();
      if (mounted) setState(() => _carteira = c);
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível carregar a carteira funcional.'));
    }
  }

  Color _corEnte() {
    final hex = _carteira?.corPrimaria;
    if (hex == null || hex.isEmpty) return const Color(0xFF1351B4);
    final v = hex.replaceFirst('#', '');
    final value = int.tryParse(v.length == 6 ? 'FF$v' : v, radix: 16);
    return value == null ? const Color(0xFF1351B4) : Color(value);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Carteira funcional')),
      body: SafeArea(
        child: _erro != null
            ? Center(child: Padding(padding: const EdgeInsets.all(24), child: Text(_erro!)))
            : _carteira == null
                ? const Center(child: CircularProgressIndicator())
                : Padding(
                    padding: const EdgeInsets.all(20),
                    child: Card(
                      elevation: 4,
                      child: Container(
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(12),
                          border: Border(top: BorderSide(color: _corEnte(), width: 8)),
                        ),
                        padding: const EdgeInsets.all(20),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(_carteira!.ente ?? 'Prefeitura',
                                style: theme.textTheme.titleMedium
                                    ?.copyWith(color: _corEnte(), fontWeight: FontWeight.bold)),
                            const SizedBox(height: 4),
                            Text('Carteira funcional', style: theme.textTheme.bodySmall),
                            const Divider(height: 24),
                            Text(_carteira!.nome,
                                style: theme.textTheme.headlineSmall
                                    ?.copyWith(fontWeight: FontWeight.bold)),
                            const SizedBox(height: 12),
                            _linha('CPF', _carteira!.cpf),
                            _linha('Matrícula', _carteira!.matricula),
                            if (_carteira!.cargo != null) _linha('Cargo', _carteira!.cargo!),
                            _linha('Regime', _carteira!.regime),
                            if (_carteira!.orgao != null) _linha('Órgão', _carteira!.orgao!),
                          ],
                        ),
                      ),
                    ),
                  ),
      ),
    );
  }

  Widget _linha(String rotulo, String valor) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(width: 96, child: Text(rotulo, style: const TextStyle(color: Colors.grey))),
          Expanded(child: Text(valor, style: const TextStyle(fontWeight: FontWeight.w600))),
        ],
      ),
    );
  }
}
