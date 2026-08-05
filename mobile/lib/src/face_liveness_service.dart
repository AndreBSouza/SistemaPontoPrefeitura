import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';

/// Deteccao facial com prova de vida (liveness) on-device via ML Kit.
class FaceLivenessService {
  final FaceDetector _detector = FaceDetector(
    options: FaceDetectorOptions(
      enableClassification: true,
      performanceMode: FaceDetectorMode.accurate,
    ),
  );

  /// Verifica se ha exatamente uma face com sinal de prova de vida
  /// (olhos abertos ou sorriso). Heuristica on-device da fundacao;
  /// liveness ativo (piscar/movimento) entra na evolucao.
  Future<bool> verificar(String caminhoImagem) async {
    final input = InputImage.fromFilePath(caminhoImagem);
    final faces = await _detector.processImage(input);
    if (faces.length != 1) return false;
    final face = faces.first;
    final olhoEsq = face.leftEyeOpenProbability ?? 0;
    final olhoDir = face.rightEyeOpenProbability ?? 0;
    final sorriso = face.smilingProbability ?? 0;
    return olhoEsq > 0.4 || olhoDir > 0.4 || sorriso > 0.5;
  }

  void dispose() => _detector.close();
}
