import 'package:flutter_test/flutter_test.dart';
import 'package:ponto_municipal_mobile/src/comando_voz.dart';

void main() {
  group('ComandoVoz.ehBater', () {
    test('reconhece variações de comando de bater ponto', () {
      expect(ComandoVoz.ehBater('bater ponto'), isTrue);
      expect(ComandoVoz.ehBater('Bater o ponto'), isTrue);
      expect(ComandoVoz.ehBater('registrar ponto'), isTrue);
      expect(ComandoVoz.ehBater('marcar meu ponto'), isTrue);
      expect(ComandoVoz.ehBater('PONTO'), isTrue);
    });

    test('ignora acentos e caixa', () {
      expect(ComandoVoz.ehBater('BÁTER PÔNTO'), isTrue);
    });

    test('não reconhece frase sem gatilho', () {
      expect(ComandoVoz.ehBater('bom dia'), isFalse);
      expect(ComandoVoz.ehBater(''), isFalse);
      expect(ComandoVoz.ehBater('qual meu saldo'), isFalse);
    });
  });

  test('normalizar remove acentos', () {
    expect(ComandoVoz.normalizar('  Áçãô  '), 'acao');
  });
}
