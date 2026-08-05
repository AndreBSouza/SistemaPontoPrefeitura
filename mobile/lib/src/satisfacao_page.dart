import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Avaliar" — pesquisa de satisfação do servidor (nota 1..5 + comentário).
class SatisfacaoPage extends StatefulWidget {
  final PontoApi api;

  const SatisfacaoPage({super.key, required this.api});

  @override
  State<SatisfacaoPage> createState() => _SatisfacaoPageState();
}

class _SatisfacaoPageState extends State<SatisfacaoPage> {
  int _nota = 0;
  final _comentarioCtrl = TextEditingController();
  bool _enviando = false;
  String? _erro;

  @override
  void dispose() {
    _comentarioCtrl.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    if (_nota == 0) {
      setState(() => _erro = 'Escolha uma nota de 1 a 5.');
      return;
    }
    setState(() {
      _enviando = true;
      _erro = null;
    });
    try {
      await widget.api
          .avaliarSatisfacao(nota: _nota, comentario: _comentarioCtrl.text.trim());
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Obrigado pela sua avaliação!')),
      );
      Navigator.of(context).pop();
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível enviar a sua avaliação.'));
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Avaliar')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text('Como tem sido sua experiência com o ponto eletrônico?',
                style: theme.textTheme.titleMedium),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(5, (i) {
                final n = i + 1;
                return IconButton(
                  iconSize: 44,
                  icon: Icon(n <= _nota ? Icons.star : Icons.star_border,
                      color: Colors.amber[700]),
                  tooltip: '$n',
                  onPressed: () => setState(() => _nota = n),
                );
              }),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _comentarioCtrl,
              maxLines: 4,
              maxLength: 500,
              decoration: const InputDecoration(
                labelText: 'Comentário (opcional)',
                border: OutlineInputBorder(),
              ),
            ),
            if (_erro != null) ...[
              const SizedBox(height: 8),
              Text(_erro!, style: TextStyle(color: theme.colorScheme.error)),
            ],
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: _enviando ? null : _enviar,
              icon: const Icon(Icons.send),
              label: Text(_enviando ? 'Enviando…' : 'Enviar avaliação'),
            ),
          ],
        ),
      ),
    );
  }
}
