import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Gestão" (app do gestor, 12.3.11): a chefia vê as justificativas pendentes do seu time
/// e aprova/rejeita pelo celular (respeitando a alçada e só do próprio time).
class GestaoPage extends StatefulWidget {
  final PontoApi api;

  const GestaoPage({super.key, required this.api});

  @override
  State<GestaoPage> createState() => _GestaoPageState();
}

class _GestaoPageState extends State<GestaoPage> {
  List<JustificativaItem>? _pendentes;
  String? _erro;
  String? _ok;
  String? _agindo;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    setState(() {
      _pendentes = null;
      _erro = null;
    });
    try {
      final p = await widget.api.pendentesDoTime();
      if (mounted) setState(() => _pendentes = p);
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível carregar as pendências da equipe.'));
    }
  }

  Future<void> _decidir(JustificativaItem j, bool aprovar) async {
    setState(() {
      _agindo = j.id;
      _erro = null;
      _ok = null;
    });
    try {
      if (aprovar) {
        await widget.api.aprovarDoTime(j.id);
      } else {
        await widget.api.rejeitarDoTime(j.id);
      }
      if (mounted) setState(() => _ok = aprovar ? 'Aprovado.' : 'Rejeitado.');
      await _carregar();
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível registrar a decisão.'));
    } finally {
      if (mounted) setState(() => _agindo = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Gestão da equipe')),
      body: RefreshIndicator(
        onRefresh: _carregar,
        child: _erro != null
            ? ListView(children: [Padding(padding: const EdgeInsets.all(24), child: Text(_erro!))])
            : _pendentes == null
                ? const Center(child: Padding(padding: EdgeInsets.all(32), child: CircularProgressIndicator()))
                : _pendentes!.isEmpty
                    ? ListView(children: const [
                        Padding(
                          padding: EdgeInsets.all(32),
                          child: Text('Nenhuma pendência do seu time no momento.',
                              textAlign: TextAlign.center),
                        )
                      ])
                    : ListView(
                        padding: const EdgeInsets.all(12),
                        children: [
                          if (_ok != null)
                            Padding(
                              padding: const EdgeInsets.only(bottom: 8),
                              child: Text(_ok!, style: const TextStyle(color: Color(0xFF168821))),
                            ),
                          ..._pendentes!.map((j) => Card(
                                child: Padding(
                                  padding: const EdgeInsets.all(12),
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text('${j.tipo} — ${j.dataInicio} a ${j.dataFim}',
                                          style: const TextStyle(fontWeight: FontWeight.w700)),
                                      if (j.motivo.isNotEmpty)
                                        Padding(
                                          padding: const EdgeInsets.only(top: 4),
                                          child: Text(j.motivo),
                                        ),
                                      const SizedBox(height: 8),
                                      Row(
                                        children: [
                                          Expanded(
                                            child: FilledButton(
                                              onPressed: _agindo == j.id ? null : () => _decidir(j, true),
                                              child: Text(_agindo == j.id ? '…' : 'Aprovar'),
                                            ),
                                          ),
                                          const SizedBox(width: 8),
                                          Expanded(
                                            child: OutlinedButton(
                                              onPressed: _agindo == j.id ? null : () => _decidir(j, false),
                                              child: const Text('Rejeitar'),
                                            ),
                                          ),
                                        ],
                                      ),
                                    ],
                                  ),
                                ),
                              )),
                        ],
                      ),
      ),
    );
  }
}
