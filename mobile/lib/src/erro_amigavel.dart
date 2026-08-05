import 'package:dio/dio.dart';

/// Traduz erros de API em mensagens que o servidor entende.
///
/// O backend responde `{"erro": "..."}` com texto de negocio (ApiExceptionHandler); sem isto o
/// app mostrava a excecao crua ("DioException [bad response]: This exception was thrown because
/// the response has a status code of 403..."), incompreensivel para o publico-alvo.
String mensagemDeErro(Object e, {String padrao = 'Nao foi possivel concluir a operacao.'}) {
  if (e is! DioException) return padrao;

  // Falha de rede (sem resposta do servidor).
  switch (e.type) {
    case DioExceptionType.connectionTimeout:
    case DioExceptionType.sendTimeout:
    case DioExceptionType.receiveTimeout:
    case DioExceptionType.connectionError:
      return 'Sem conexao com o servidor. Verifique a internet e tente de novo.';
    default:
      break;
  }

  // Mensagem de negocio vinda do backend.
  final dados = e.response?.data;
  if (dados is Map) {
    final texto = dados['erro'] ?? dados['message'];
    if (texto is String && texto.trim().isNotEmpty) return texto;
  }

  switch (e.response?.statusCode) {
    case 400:
      return 'Dados invalidos. Confira as informacoes e tente de novo.';
    case 401:
      return 'Este aparelho nao esta mais autorizado. Solicite um novo codigo ao RH.';
    case 403:
      return 'Voce nao tem permissao para esta acao.';
    case 404:
      return 'Registro nao encontrado.';
    case 409:
      return 'Esta operacao ja foi feita ou conflita com outra.';
    case 429:
      return 'Muitas tentativas. Aguarde alguns minutos e tente de novo.';
    default:
      return padrao;
  }
}

/// true quando o backend indicou que o dispositivo perdeu a autorizacao (revogado pelo RH).
bool dispositivoRevogado(Object e) =>
    e is DioException && e.response?.statusCode == 401;
