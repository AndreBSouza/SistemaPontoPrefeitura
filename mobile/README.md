# Ponto Municipal — Mobile (Flutter)

App do servidor/gestor: registro de ponto **offline-first**, **geolocalização/geofencing**,
**biometria facial com liveness** (on-device via ML Kit), espelho de ponto, solicitação de
abono/justificativa e notificações push.

## Estado atual

- ✅ Esqueleto Flutter gerado (`flutter create`, org `br.gov.ponto`)
- ✅ `flutter analyze` sem issues
- ✅ **`app-debug.apk` builda** (Android) com os plugins nativos (camera, ML Kit, geolocator)
- ✅ Implementado: **registro offline-first** (`OfflineQueue` + `SyncService`/connectivity),
  **geolocalização/geofencing** (`GeoService`), **biometria facial com liveness** on-device
  (`FaceLivenessService`/ML Kit + `CapturaFacePage`) e **consentimento LGPD** (`/api/lgpd`)
- ⏳ iOS: requer **macOS** (Mac físico/VM) ou **CI com runner macOS** (Codemagic, GitHub Actions, Bitrise)
- 🔧 Build Android: o módulo `camera_android_camerax` exige `androidx.concurrent:concurrent-futures`
  — injetado em [`android/build.gradle.kts`](android/build.gradle.kts)
- ⏭️ Pendente: cifragem da fila offline (secure storage/Drift), enrollment + comparação 1:1 da
  biometria, login OIDC (flutter_appauth), notificações push (FCM). Plugins extras em
  [`pubspec.planned.yaml`](pubspec.planned.yaml).

## Rodar

```bash
cd mobile
flutter pub get
flutter run                 # em emulador/dispositivo Android
flutter build apk --debug   # gera build/app/outputs/flutter-apk/app-debug.apk
```

Ambiente verificado: Flutter 3.44.3 (stable) · Android SDK (platform 35/36, build-tools 35/36,
NDK 28.2, CMake 3.22.1) · Java 25.

## Próximos passos (tasks 5.x)

- Fila local cifrada (Drift/SQLite) e sincronização em background (idempotente)
- Captura facial + liveness (ML Kit) e cadastro da referência biométrica (consentimento LGPD)
- Geofencing por local de trabalho
- Autenticação OIDC (Keycloak) e contexto de tenant
- Comprovante de registro (NSR) e espelho de ponto
