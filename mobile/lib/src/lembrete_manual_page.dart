import 'package:flutter/material.dart';

import 'lembrete_service.dart';

/// Tela de fallback para configurar um lembrete manual quando não há jornada
/// disponível no backend (sem conexão, vínculo sem escala, etc.).
class LembreteManualPage extends StatefulWidget {
  final LembreteService lembreteService;

  const LembreteManualPage({super.key, required this.lembreteService});

  @override
  State<LembreteManualPage> createState() => _LembreteManualPageState();
}

class _LembreteManualPageState extends State<LembreteManualPage> {
  TimeOfDay _horario = const TimeOfDay(hour: 8, minute: 0);
  bool _ocupado = false;
  bool _salvo = false;

  Future<void> _escolherHorario() async {
    final escolhido = await showTimePicker(
      context: context,
      initialTime: _horario,
      helpText: 'Horário do lembrete',
    );
    if (escolhido != null) setState(() => _horario = escolhido);
  }

  Future<void> _salvar() async {
    setState(() {
      _ocupado = true;
      _salvo = false;
    });
    try {
      await widget.lembreteService.agendarManual(
        hora: _horario.hour,
        minuto: _horario.minute,
        titulo: 'Hora de registrar o ponto!',
      );
      if (mounted) setState(() => _salvo = true);
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Lembrete de ponto')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Configurar lembrete manual',
                style: theme.textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              Text(
                'Não foi possível obter sua jornada automaticamente. '
                'Defina manualmente o horário do lembrete de ponto.',
                style: theme.textTheme.bodyLarge,
              ),
              const SizedBox(height: 32),
              OutlinedButton.icon(
                onPressed: _escolherHorario,
                icon: const Icon(Icons.access_time),
                label: Text(
                  'Horário: ${_horario.format(context)}',
                  style: const TextStyle(fontSize: 20),
                ),
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size.fromHeight(64),
                ),
              ),
              const SizedBox(height: 24),
              if (_salvo)
                Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: Row(
                    children: [
                      Icon(Icons.check_circle,
                          color: Colors.green[700], size: 24),
                      const SizedBox(width: 8),
                      Text(
                        'Lembrete agendado para ${_horario.format(context)}!',
                        style: TextStyle(
                            color: Colors.green[700],
                            fontSize: 16,
                            fontWeight: FontWeight.w600),
                      ),
                    ],
                  ),
                ),
              ElevatedButton.icon(
                onPressed: _ocupado ? null : _salvar,
                icon: _ocupado
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.notifications_active_outlined),
                label: const Text('Salvar lembrete'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
