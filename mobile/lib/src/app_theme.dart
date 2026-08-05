import 'package:flutter/material.dart';

/// Tema acessível do Ponto Municipal: gov.br azul, alto contraste, fontes grandes.
class AppTheme {
  AppTheme._();

  static const Color _govBlue = Color(0xFF1351B4);

  /// Cor primária padrão (azul gov.br) — usada antes de carregar o branding do ente.
  static const Color govBlue = _govBlue;

  static const Color _govBlueDark = Color(0xFF0D3D87);
  static const Color _sucesso = Color(0xFF1A7A1A);
  static const Color _erro = Color(0xFFB71C1C);

  static ThemeData get theme => themeFor(_govBlue);

  /// Tema com a cor primária do ente (white-label); [primary] vem do branding.
  static ThemeData themeFor(Color primary) {
    final base = ColorScheme.fromSeed(
      seedColor: primary,
      brightness: Brightness.light,
      primary: primary,
      onPrimary: Colors.white,
      secondary: _govBlueDark,
      error: _erro,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: base,
      // --- Tipografia grande para idosos ---
      textTheme: const TextTheme(
        displayLarge: TextStyle(fontSize: 57, fontWeight: FontWeight.bold),
        displayMedium: TextStyle(fontSize: 45, fontWeight: FontWeight.bold),
        displaySmall: TextStyle(fontSize: 36, fontWeight: FontWeight.bold),
        headlineLarge: TextStyle(fontSize: 32, fontWeight: FontWeight.bold),
        headlineMedium: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
        headlineSmall: TextStyle(fontSize: 24, fontWeight: FontWeight.w600),
        titleLarge: TextStyle(fontSize: 22, fontWeight: FontWeight.w600),
        titleMedium: TextStyle(fontSize: 18, fontWeight: FontWeight.w500),
        titleSmall: TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
        bodyLarge: TextStyle(fontSize: 18),
        bodyMedium: TextStyle(fontSize: 16),
        bodySmall: TextStyle(fontSize: 14),
        labelLarge: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
        labelMedium: TextStyle(fontSize: 16),
        labelSmall: TextStyle(fontSize: 14),
      ),
      // --- Botões grandes ---
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          minimumSize: const Size.fromHeight(56),
          textStyle: const TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size.fromHeight(48),
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        ),
      ),
      // --- Campos de texto ---
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(),
        contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        labelStyle: TextStyle(fontSize: 16),
      ),
      // --- AppBar ---
      appBarTheme: AppBarTheme(
        backgroundColor: primary,
        foregroundColor: Colors.white,
        titleTextStyle: const TextStyle(
          fontSize: 20,
          fontWeight: FontWeight.bold,
          color: Colors.white,
        ),
        elevation: 2,
      ),
      // --- Focus visível para acessibilidade ---
      focusColor: primary.withAlpha(51),
      // Extensões de cor para uso nos widgets
      extensions: const [PontoColors(sucesso: _sucesso, erro: _erro)],
    );
  }
}

/// Extensão de cores semânticas para uso direto no app.
class PontoColors extends ThemeExtension<PontoColors> {
  const PontoColors({required this.sucesso, required this.erro});

  final Color sucesso;
  final Color erro;

  @override
  PontoColors copyWith({Color? sucesso, Color? erro}) =>
      PontoColors(sucesso: sucesso ?? this.sucesso, erro: erro ?? this.erro);

  @override
  PontoColors lerp(PontoColors? other, double t) {
    if (other == null) return this;
    return PontoColors(
      sucesso: Color.lerp(sucesso, other.sucesso, t)!,
      erro: Color.lerp(erro, other.erro, t)!,
    );
  }
}
