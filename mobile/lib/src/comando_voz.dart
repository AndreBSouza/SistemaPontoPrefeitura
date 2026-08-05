/// Reconhecimento de comando de voz — lógica PURA (sem plugins), para ser testável.
///
/// Acessibilidade (12.4.13): idosos/PCD podem bater o ponto por voz. Aqui só decidimos se a
/// fala reconhecida é um pedido de "bater ponto"; a captura de áudio fica no [VozService].
class ComandoVoz {
  ComandoVoz._();

  static const List<String> _gatilhos = ['bater', 'registrar', 'marcar', 'ponto', 'baté'];

  /// A frase reconhecida pede para bater o ponto?
  static bool ehBater(String texto) {
    final t = normalizar(texto);
    if (t.isEmpty) return false;
    return _gatilhos.any(t.contains);
  }

  /// Minúsculas, sem acento e sem espaços nas pontas — casa "Bater", "báter", "PONTO".
  static String normalizar(String s) {
    return s
        .toLowerCase()
        .replaceAll(RegExp('[áàâãä]'), 'a')
        .replaceAll(RegExp('[éèêë]'), 'e')
        .replaceAll(RegExp('[íìîï]'), 'i')
        .replaceAll(RegExp('[óòôõö]'), 'o')
        .replaceAll(RegExp('[úùûü]'), 'u')
        .replaceAll('ç', 'c')
        .trim();
  }
}
