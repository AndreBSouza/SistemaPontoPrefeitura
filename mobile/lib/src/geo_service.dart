import 'package:geolocator/geolocator.dart';

/// Captura de localizacao para o registro de ponto. A verificacao de area
/// (geofence) e feita no servidor e e apenas informativa para o administrador.
class GeoService {
  Future<Position?> posicaoAtual() async {
    if (!await Geolocator.isLocationServiceEnabled()) return null;
    var permissao = await Geolocator.checkPermission();
    if (permissao == LocationPermission.denied) {
      permissao = await Geolocator.requestPermission();
    }
    if (permissao == LocationPermission.denied ||
        permissao == LocationPermission.deniedForever) {
      return null;
    }
    // A localizacao e OPCIONAL (verificacao silenciosa do admin), entao a batida nunca pode
    // ficar presa esperando um fix de GPS: sem sinal em poucos segundos, segue sem coordenada.
    try {
      return await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(timeLimit: Duration(seconds: 8)),
      );
    } catch (_) {
      return await Geolocator.getLastKnownPosition();
    }
  }
}
