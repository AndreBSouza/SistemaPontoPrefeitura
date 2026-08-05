import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Teste de widget do padrão de consentimento (toggle) usado na tela de
/// Privacidade (LGPD): renderiza o rótulo e alterna o valor ao tocar.
void main() {
  testWidgets('toggle de consentimento alterna o valor', (WidgetTester tester) async {
    bool concedido = false;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: StatefulBuilder(
          builder: (context, setState) => SwitchListTile(
            title: const Text('Uso de biometria facial'),
            value: concedido,
            onChanged: (v) => setState(() => concedido = v),
          ),
        ),
      ),
    ));

    expect(find.text('Uso de biometria facial'), findsOneWidget);
    expect(concedido, isFalse);

    await tester.tap(find.byType(SwitchListTile));
    await tester.pumpAndSettle();

    expect(concedido, isTrue);
  });
}
