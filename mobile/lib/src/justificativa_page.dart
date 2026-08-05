import 'package:flutter/material.dart';

import 'erro_amigavel.dart';
import 'ponto_api.dart';

/// Tela "Solicitar atestado/justificativa" — período + motivo + tipo.
class JustificativaPage extends StatefulWidget {
  final PontoApi api;
  final String vinculoId;

  const JustificativaPage({
    super.key,
    required this.api,
    required this.vinculoId,
  });

  @override
  State<JustificativaPage> createState() => _JustificativaPageState();
}

class _JustificativaPageState extends State<JustificativaPage> {
  // Tipos de justificativa — valores reais do enum TipoJustificativa do backend.
  static const _tipos = [
    'ATESTADO',
    'LICENCA',
    'FERIAS',
    'FALTA',
    'ATRASO',
    'SAIDA_ANTECIPADA',
    'OUTRO',
  ];

  static const _rotulosTipos = {
    'ATESTADO': 'Atestado médico',
    'LICENCA': 'Licença',
    'FERIAS': 'Férias',
    'FALTA': 'Falta justificada',
    'ATRASO': 'Atraso',
    'SAIDA_ANTECIPADA': 'Saída antecipada',
    'OUTRO': 'Outro',
  };

  final _formKey = GlobalKey<FormState>();
  final _motivoCtrl = TextEditingController();
  String _tipoSelecionado = _tipos.first;
  DateTime? _dataInicio;
  DateTime? _dataFim;
  bool _ocupado = false;
  String? _sucesso;
  String? _erro;

  @override
  void dispose() {
    _motivoCtrl.dispose();
    super.dispose();
  }

  Future<void> _selecionarData({required bool isInicio}) async {
    final inicial = isInicio ? _dataInicio : _dataFim;
    final picked = await showDatePicker(
      context: context,
      initialDate: inicial ?? DateTime.now(),
      firstDate: DateTime.now().subtract(const Duration(days: 365)),
      lastDate: DateTime.now(),
      locale: const Locale('pt', 'BR'),
      helpText: isInicio ? 'Data de início' : 'Data de fim',
      cancelText: 'Cancelar',
      confirmText: 'Confirmar',
    );
    if (picked != null) {
      setState(() {
        if (isInicio) {
          _dataInicio = picked;
          if (_dataFim != null && _dataFim!.isBefore(picked)) {
            _dataFim = picked;
          }
        } else {
          _dataFim = picked;
        }
      });
    }
  }

  String _formatarData(DateTime? d) {
    if (d == null) return 'Selecionar';
    return '${d.day.toString().padLeft(2, '0')}/'
        '${d.month.toString().padLeft(2, '0')}/'
        '${d.year}';
  }

  String _toIso(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Future<void> _enviar() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    if (_dataInicio == null || _dataFim == null) {
      setState(() => _erro = 'Selecione o período (início e fim).');
      return;
    }
    setState(() {
      _ocupado = true;
      _sucesso = null;
      _erro = null;
    });
    try {
      await widget.api.solicitarJustificativa(
        vinculoId: widget.vinculoId,
        tipo: _tipoSelecionado,
        dataInicio: _toIso(_dataInicio!),
        dataFim: _toIso(_dataFim!),
        motivo: _motivoCtrl.text.trim().isEmpty ? null : _motivoCtrl.text.trim(),
      );
      if (mounted) {
        setState(() => _sucesso = 'Justificativa enviada com sucesso!');
        _motivoCtrl.clear();
        _dataInicio = null;
        _dataFim = null;
      }
    } catch (e) {
      if (mounted) setState(() => _erro = mensagemDeErro(e, padrao: 'Não foi possível enviar a justificativa.'));
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Solicitar justificativa')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('Tipo', style: theme.textTheme.titleMedium),
                const SizedBox(height: 8),
                DropdownButtonFormField<String>(
                  initialValue: _tipoSelecionado,
                  isExpanded: true,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(
                        horizontal: 16, vertical: 14),
                  ),
                  items: _tipos
                      .map((t) => DropdownMenuItem(
                            value: t,
                            child: Text(
                              _rotulosTipos[t] ?? t,
                              style: const TextStyle(fontSize: 16),
                            ),
                          ))
                      .toList(),
                  onChanged: (v) {
                    if (v != null) setState(() => _tipoSelecionado = v);
                  },
                ),
                const SizedBox(height: 20),
                Text('Período', style: theme.textTheme.titleMedium),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () => _selecionarData(isInicio: true),
                        icon: const Icon(Icons.calendar_today, size: 20),
                        label: Text('Início: ${_formatarData(_dataInicio)}'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () => _selecionarData(isInicio: false),
                        icon: const Icon(Icons.calendar_today, size: 20),
                        label: Text('Fim: ${_formatarData(_dataFim)}'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                Text('Motivo (opcional)', style: theme.textTheme.titleMedium),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _motivoCtrl,
                  maxLines: 3,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    hintText: 'Descreva brevemente o motivo...',
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Anexo: encaminhe o documento ao setor de RH.',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: Colors.grey[600]),
                ),
                const SizedBox(height: 24),
                if (_sucesso != null) ...[
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: Colors.green[50],
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: Colors.green),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.check_circle, color: Colors.green, size: 24),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            _sucesso!,
                            style: const TextStyle(
                                color: Colors.green,
                                fontSize: 16,
                                fontWeight: FontWeight.w600),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                if (_erro != null) ...[
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: Colors.red[50],
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: Colors.red),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.error_outline,
                            color: theme.colorScheme.error, size: 24),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            _erro!,
                            style: TextStyle(
                                color: theme.colorScheme.error, fontSize: 16),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                ],
                ElevatedButton.icon(
                  onPressed: _ocupado ? null : _enviar,
                  icon: _ocupado
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.send),
                  label: const Text('Enviar justificativa'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
