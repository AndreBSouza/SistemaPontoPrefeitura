import 'package:flutter_tts/flutter_tts.dart';
import 'package:speech_to_text/speech_to_text.dart';

import 'comando_voz.dart';

/// Acessibilidade por voz (12.4.13): fala o status em voz alta (TTS) e ouve o comando de bater
/// ponto (STT). Idosos/PCD usam sem depender de ler a tela. O reconhecimento do comando fica em
/// [ComandoVoz] (lógica pura, testada); aqui é só a integração com os plugins.
class VozService {
  final FlutterTts _tts = FlutterTts();
  final SpeechToText _stt = SpeechToText();
  bool _sttPronto = false;

  /// Fala o texto em português do Brasil, num ritmo pausado (idosos).
  Future<void> falar(String texto) async {
    if (texto.trim().isEmpty) return;
    await _tts.setLanguage('pt-BR');
    await _tts.setSpeechRate(0.45);
    await _tts.speak(texto);
  }

  /// Ouve uma frase curta e diz se foi um comando de "bater ponto".
  /// Retorna `null` quando não foi possível ouvir (sem permissão/serviço).
  Future<bool?> ouvirComandoBater({Duration duracao = const Duration(seconds: 5)}) async {
    _sttPronto = _sttPronto || await _stt.initialize();
    if (!_sttPronto) return null;

    var reconhecido = '';
    await _stt.listen(
      onResult: (resultado) => reconhecido = resultado.recognizedWords,
      listenOptions: SpeechListenOptions(listenFor: duracao, localeId: 'pt_BR'),
    );
    await Future<void>.delayed(duracao + const Duration(milliseconds: 400));
    await _stt.stop();

    if (reconhecido.trim().isEmpty) return null;
    return ComandoVoz.ehBater(reconhecido);
  }

  Future<void> parar() async {
    await _tts.stop();
    await _stt.stop();
  }
}
