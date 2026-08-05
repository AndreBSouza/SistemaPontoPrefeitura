import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:timezone/timezone.dart' as tz;

import 'src/app_theme.dart';
import 'src/home_page.dart';
import 'src/lembrete_service.dart';

/// Navigator global — necessário para rotear o tap da notificação quando o app
/// está em background ou encerrado.
final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

/// Cor primária do tema, configurável por ente (white-label). Atualizada quando
/// o app carrega a identidade visual via /api/me/branding.
final ValueNotifier<Color> corPrimariaApp = ValueNotifier<Color>(AppTheme.govBlue);

/// Instância global do plugin de notificações (inicializada em main).
final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
    FlutterLocalNotificationsPlugin();

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // --- Timezone ---
  tz_data.initializeTimeZones();
  tz.setLocalLocation(tz.getLocation('America/Sao_Paulo'));

  // --- Flutter Local Notifications ---
  const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
  const iosInit = DarwinInitializationSettings(
    requestAlertPermission: true,
    requestBadgePermission: true,
    requestSoundPermission: true,
  );
  await flutterLocalNotificationsPlugin.initialize(
    const InitializationSettings(android: androidInit, iOS: iosInit),
    onDidReceiveNotificationResponse: _onNotificationTap,
    onDidReceiveBackgroundNotificationResponse: _onNotificationTapBackground,
  );

  // Solicitar permissão Android 13+
  await flutterLocalNotificationsPlugin
      .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin>()
      ?.requestNotificationsPermission();

  // Criar canal Android com som personalizado (importância MAX)
  const androidChannel = AndroidNotificationChannel(
    kCanalId,
    kCanalNome,
    description: kCanalDesc,
    importance: Importance.max,
    sound: RawResourceAndroidNotificationSound('alerta_ponto'),
    playSound: true,
    enableVibration: true,
  );
  await flutterLocalNotificationsPlugin
      .resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin>()
      ?.createNotificationChannel(androidChannel);

  runApp(const PontoApp());
}

/// Registrado pela HomePage: ao tocar o lembrete, abre a verificação facial e
/// bate o ponto. Fica nulo enquanto a HomePage não está montada.
void Function()? aoTocarLembrete;

/// Callback chamado quando o usuário toca na notificação (app em foreground ou background).
void _onNotificationTap(NotificationResponse response) {
  if (response.payload == kPayloadBater) {
    aoTocarLembrete?.call();
  }
}

/// Callback para tap em notificação quando o app está encerrado (top-level function).
@pragma('vm:entry-point')
void _onNotificationTapBackground(NotificationResponse response) {
  // O app será aberto normalmente pelo sistema; a rota '/bater' é tratada pelo
  // navigatorKey quando o widget estiver montado.
}

class PontoApp extends StatelessWidget {
  const PontoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<Color>(
      valueListenable: corPrimariaApp,
      builder: (context, cor, _) => MaterialApp(
        title: 'Ponto Municipal',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.themeFor(cor),
        navigatorKey: navigatorKey,
        home: const HomePage(),
      ),
    );
  }
}
