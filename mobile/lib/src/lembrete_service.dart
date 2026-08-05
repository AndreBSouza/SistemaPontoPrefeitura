import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/timezone.dart' as tz;

import 'ponto_api.dart';

/// Canal Android para lembretes de batida (importância máxima + som próprio).
const String kCanalId = 'ponto_lembretes';
const String kCanalNome = 'Lembretes de batida';
const String kCanalDesc = 'Avisa na hora de bater o ponto';

/// Payload da notificação — identificado no tap.
const String kPayloadBater = 'bater_ponto';

/// IDs fixos de notificação por dia da semana × tipo (entrada/saída).
/// diaSemana 1..7 × tipo 0 (entrada) / 1 (saída) → id único 0..13.
int _idNotificacao(int diaSemana, int tipo) => (diaSemana - 1) * 2 + tipo;

/// Serviço de lembretes locais agendados para batida de ponto.
///
/// Usa [flutter_local_notifications] com canal Android dedicado cujo som é
/// o arquivo raw `alerta_ponto` (não o padrão do sistema).
class LembreteService {
  LembreteService(this._plugin);

  final FlutterLocalNotificationsPlugin _plugin;

  // -------------------------------------------------------------------------
  // Detalhes de plataforma
  // -------------------------------------------------------------------------

  static const DarwinNotificationDetails _iosDetails = DarwinNotificationDetails(
    sound: 'alerta_ponto.wav',
    presentSound: true,
    interruptionLevel: InterruptionLevel.timeSensitive,
  );

  static const NotificationDetails _detalhes = NotificationDetails(
    android: AndroidNotificationDetails(
      kCanalId,
      kCanalNome,
      channelDescription: kCanalDesc,
      importance: Importance.max,
      priority: Priority.high,
      sound: RawResourceAndroidNotificationSound('alerta_ponto'),
      playSound: true,
      enableVibration: true,
    ),
    iOS: _iosDetails,
  );

  // -------------------------------------------------------------------------
  // Cancelar todos os lembretes
  // -------------------------------------------------------------------------

  Future<void> cancelarTodos() async {
    await _plugin.cancelAll();
  }

  // -------------------------------------------------------------------------
  // Agendar a partir dos horários do backend
  // -------------------------------------------------------------------------

  /// Agenda lembretes diários para cada [HorarioDia] da jornada.
  ///
  /// Cancela agendamentos anteriores antes de criar os novos.
  Future<void> agendarDaJornada(List<HorarioDia> horarios) async {
    await cancelarTodos();
    final brasilia = tz.getLocation('America/Sao_Paulo');
    for (final h in horarios) {
      await _agendarHorario(
        id: _idNotificacao(h.diaSemana, 0),
        diaSemana: h.diaSemana,
        hora: _parseHora(h.horaEntrada),
        titulo: 'Hora de registrar sua entrada!',
        corpo: 'Toque para abrir a verificação facial e bater o ponto.',
        brasilia: brasilia,
      );
      await _agendarHorario(
        id: _idNotificacao(h.diaSemana, 1),
        diaSemana: h.diaSemana,
        hora: _parseHora(h.horaSaida),
        titulo: 'Hora de registrar sua saída!',
        corpo: 'Toque para abrir a verificação facial e bater o ponto.',
        brasilia: brasilia,
      );
    }
  }

  /// Agenda um único lembrete diário manual (fallback quando não há jornada).
  Future<void> agendarManual({
    required int hora,
    required int minuto,
    required String titulo,
  }) async {
    final brasilia = tz.getLocation('America/Sao_Paulo');
    // Usa ID fixo 99 para o lembrete manual
    await _agendarHorario(
      id: 99,
      diaSemana: null, // toda semana
      hora: _HoraMinuto(hora, minuto),
      titulo: titulo,
      corpo: 'Toque para verificação facial e registro de ponto.',
      brasilia: brasilia,
    );
  }

  // -------------------------------------------------------------------------
  // Helpers privados
  // -------------------------------------------------------------------------

  _HoraMinuto _parseHora(String s) {
    // Formato: "HH:mm:ss" ou "HH:mm"
    final partes = s.split(':');
    return _HoraMinuto(
      int.tryParse(partes.isNotEmpty ? partes[0] : '8') ?? 8,
      int.tryParse(partes.length > 1 ? partes[1] : '0') ?? 0,
    );
  }

  Future<void> _agendarHorario({
    required int id,
    required int? diaSemana, // ISO 1=Seg..7=Dom; null = todos os dias
    required _HoraMinuto hora,
    required String titulo,
    required String corpo,
    required tz.Location brasilia,
  }) async {
    final agora = tz.TZDateTime.now(brasilia);
    var proximo = tz.TZDateTime(
      brasilia,
      agora.year,
      agora.month,
      agora.day,
      hora.hora,
      hora.minuto,
    );
    if (proximo.isBefore(agora)) {
      proximo = proximo.add(const Duration(days: 1));
    }

    // Avança até o dia da semana correto (1=Mon..7=Sun → Dart weekday 1=Mon..7=Sun)
    if (diaSemana != null) {
      while (proximo.weekday != diaSemana) {
        proximo = proximo.add(const Duration(days: 1));
      }
    }

    await _plugin.zonedSchedule(
      id,
      titulo,
      corpo,
      proximo,
      _detalhes,
      androidScheduleMode: AndroidScheduleMode.inexactAllowWhileIdle,
      uiLocalNotificationDateInterpretation:
          UILocalNotificationDateInterpretation.absoluteTime,
      matchDateTimeComponents: diaSemana != null
          ? DateTimeComponents.dayOfWeekAndTime
          : DateTimeComponents.time,
      payload: kPayloadBater,
    );
  }
}

class _HoraMinuto {
  final int hora;
  final int minuto;
  const _HoraMinuto(this.hora, this.minuto);
}
