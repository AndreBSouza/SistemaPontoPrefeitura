import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Esqueci de bater" — o servidor solicita uma correção de marcação,
/// que vai para aprovação da chefia/RH (não cria a batida diretamente).
class EsqueciBaterPage extends StatefulWidget {
  final PontoApi api;

  const EsqueciBaterPage({super.key, required this.api});

  @override
  State<EsqueciBaterPage> createState() => _EsqueciBaterPageState();
}

const _tipos = <String, String>{
  'ENTRADA': 'Entrada',
  'SAIDA': 'Saída',
  'INTERVALO_INICIO': 'Início do intervalo',
  'INTERVALO_FIM': 'Fim do intervalo',
};

/// O backend devolve Instant em UTC. Exibir o texto cru faria a solicitação das 08:00 aparecer
/// como 11:00 (UTC-3) e o servidor achar que pediu a hora errada.
String _dataHoraLocal(String instante) {
  final d = DateTime.tryParse(instante)?.toLocal();
  if (d == null) return instante;
  String dois(int n) => n.toString().padLeft(2, '0');
  return '${dois(d.day)}/${dois(d.month)}/${d.year} ${dois(d.hour)}:${dois(d.minute)}';
}

class _EsqueciBaterPageState extends State<EsqueciBaterPage> {
  DateTime _data = DateTime.now();
  TimeOfDay _hora = TimeOfDay.now();
  String _tipo = 'ENTRADA';
  final _motivoCtrl = TextEditingController();
  bool _enviando = false;
  String? _erro;
  List<CorrecaoItem>? _minhas;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void dispose() {
    _motivoCtrl.dispose();
    super.dispose();
  }

  Future<void> _carregar() async {
    try {
      final lista = await widget.api.listarCorrecoes();
      if (mounted) setState(() => _minhas = lista);
    } catch (_) {
      // lista é secundária; não bloqueia o formulário
    }
  }

  Future<void> _enviar() async {
    if (_motivoCtrl.text.trim().isEmpty) {
      setState(() => _erro = 'Informe o motivo.');
      return;
    }
    setState(() {
      _enviando = true;
      _erro = null;
    });
    try {
      final dataHora = DateTime(
          _data.year, _data.month, _data.day, _hora.hour, _hora.minute);
      await widget.api.solicitarCorrecao(
        dataHora: dataHora,
        tipo: _tipo,
        motivo: _motivoCtrl.text.trim(),
      );
      if (!mounted) return;
      _motivoCtrl.clear();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Solicitação enviada para aprovação.')),
      );
      await _carregar();
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível enviar a solicitação.'));
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final dataLabel =
        '${_data.day.toString().padLeft(2, '0')}/${_data.month.toString().padLeft(2, '0')}/${_data.year}';
    return Scaffold(
      appBar: AppBar(title: const Text('Esqueci de bater')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text('Solicite a correção de uma marcação esquecida.',
                style: theme.textTheme.bodyLarge),
            const SizedBox(height: 20),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.event),
              title: const Text('Data'),
              subtitle: Text(dataLabel, style: theme.textTheme.titleMedium),
              trailing: const Icon(Icons.edit),
              onTap: () async {
                final d = await showDatePicker(
                  context: context,
                  initialDate: _data,
                  firstDate: DateTime(_data.year - 1),
                  lastDate: DateTime.now(),
                );
                if (d != null) setState(() => _data = d);
              },
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.schedule),
              title: const Text('Hora'),
              subtitle: Text(_hora.format(context),
                  style: theme.textTheme.titleMedium),
              trailing: const Icon(Icons.edit),
              onTap: () async {
                final t =
                    await showTimePicker(context: context, initialTime: _hora);
                if (t != null) setState(() => _hora = t);
              },
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _tipo,
              decoration: const InputDecoration(
                labelText: 'Tipo de marcação',
                border: OutlineInputBorder(),
              ),
              items: _tipos.entries
                  .map((e) =>
                      DropdownMenuItem(value: e.key, child: Text(e.value)))
                  .toList(),
              onChanged: (v) => setState(() => _tipo = v ?? 'ENTRADA'),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _motivoCtrl,
              maxLines: 3,
              maxLength: 500,
              decoration: const InputDecoration(
                labelText: 'Motivo',
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
              label: Text(_enviando ? 'Enviando…' : 'Solicitar correção'),
            ),
            const SizedBox(height: 24),
            if (_minhas != null && _minhas!.isNotEmpty) ...[
              Text('Minhas solicitações', style: theme.textTheme.titleMedium),
              const Divider(),
              ..._minhas!.map((c) => ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.history),
                    title: Text('${_tipos[c.tipo] ?? c.tipo} — ${_dataHoraLocal(c.dataHora)}'),
                    subtitle: Text(c.motivo),
                    trailing: Text(c.status),
                  )),
            ],
          ],
        ),
      ),
    );
  }
}
