import 'package:flutter_test/flutter_test.dart';
import 'package:ponto_municipal_mobile/src/ponto_api.dart';

void main() {
  group('DadosTitular.fromJson (LGPD)', () {
    test('parseia os campos do titular', () {
      final d = DadosTitular.fromJson(<String, dynamic>{
        'nome': 'Maria',
        'cpf': '12345678901',
        'email': 'maria@ente.gov.br',
        'totalVinculos': 2,
        'totalRegistros': 40,
      });
      expect(d.nome, 'Maria');
      expect(d.cpf, '12345678901');
      expect(d.email, 'maria@ente.gov.br');
      expect(d.totalVinculos, 2);
      expect(d.totalRegistros, 40);
    });

    test('tolera campos ausentes ou nulos', () {
      final d = DadosTitular.fromJson(<String, dynamic>{});
      expect(d.nome, '');
      expect(d.cpf, '');
      expect(d.email, isNull);
      expect(d.totalVinculos, 0);
      expect(d.totalRegistros, 0);
    });
  });

  group('ComunicadoItem.fromJson (broadcast)', () {
    test('parseia os campos do comunicado', () {
      final c = ComunicadoItem.fromJson(<String, dynamic>{
        'id': 'c-1',
        'titulo': 'Recesso',
        'mensagem': 'Expediente reduzido.',
        'geral': true,
        'publicadoEm': '2026-06-25T12:00:00Z',
      });
      expect(c.id, 'c-1');
      expect(c.titulo, 'Recesso');
      expect(c.mensagem, 'Expediente reduzido.');
      expect(c.geral, isTrue);
      expect(c.publicadoEm, '2026-06-25T12:00:00Z');
    });

    test('tolera campos ausentes (geral assume verdadeiro)', () {
      final c = ComunicadoItem.fromJson(<String, dynamic>{});
      expect(c.id, '');
      expect(c.titulo, '');
      expect(c.mensagem, '');
      expect(c.geral, isTrue);
      expect(c.publicadoEm, '');
    });
  });

  group('ResumoMe.fromJson (a seu favor)', () {
    test('parseia saldo e hora extra da semana', () {
      final r = ResumoMe.fromJson(<String, dynamic>{
        'saldoBancoHorasMinutos': 130,
        'horaExtraSemanaMinutos': 45,
      });
      expect(r.saldoBancoHorasMinutos, 130);
      expect(r.horaExtraSemanaMinutos, 45);
    });

    test('tolera campos ausentes (assume zero)', () {
      final r = ResumoMe.fromJson(<String, dynamic>{});
      expect(r.saldoBancoHorasMinutos, 0);
      expect(r.horaExtraSemanaMinutos, 0);
    });
  });

  group('CorrecaoItem.fromJson (esqueci de bater)', () {
    test('parseia a solicitação de correção', () {
      final c = CorrecaoItem.fromJson(<String, dynamic>{
        'id': 'cor-1',
        'dataHora': '2026-06-24T11:00:00Z',
        'tipo': 'ENTRADA',
        'motivo': 'Esqueci',
        'status': 'PENDENTE',
      });
      expect(c.id, 'cor-1');
      expect(c.dataHora, '2026-06-24T11:00:00Z');
      expect(c.tipo, 'ENTRADA');
      expect(c.motivo, 'Esqueci');
      expect(c.status, 'PENDENTE');
    });

    test('tolera campos ausentes', () {
      final c = CorrecaoItem.fromJson(<String, dynamic>{});
      expect(c.id, '');
      expect(c.tipo, '');
      expect(c.status, '');
    });
  });

  group('CarteiraDigital.fromJson', () {
    test('parseia a carteira funcional', () {
      final c = CarteiraDigital.fromJson(<String, dynamic>{
        'nome': 'Marta',
        'cpf': '90909090909',
        'matricula': 'M-7',
        'cargo': 'Fiscal',
        'regime': 'ESTATUTARIO',
        'orgao': 'Secretaria de Obras',
        'ente': 'Prefeitura X',
        'corPrimaria': '#1351B4',
      });
      expect(c.nome, 'Marta');
      expect(c.matricula, 'M-7');
      expect(c.regime, 'ESTATUTARIO');
      expect(c.orgao, 'Secretaria de Obras');
      expect(c.ente, 'Prefeitura X');
    });

    test('tolera campos ausentes', () {
      final c = CarteiraDigital.fromJson(<String, dynamic>{});
      expect(c.nome, '');
      expect(c.cargo, isNull);
      expect(c.orgao, isNull);
    });
  });

  group('AusenciaItem.fromJson (férias)', () {
    test('parseia a ausência programada', () {
      final a = AusenciaItem.fromJson(<String, dynamic>{
        'id': 'aus-1',
        'tipo': 'FERIAS',
        'dataInicio': '2026-07-01',
        'dataFim': '2026-07-30',
        'dias': 30,
        'observacao': 'Férias regulamentares',
      });
      expect(a.id, 'aus-1');
      expect(a.tipo, 'FERIAS');
      expect(a.dataInicio, '2026-07-01');
      expect(a.dataFim, '2026-07-30');
      expect(a.dias, 30);
      expect(a.observacao, 'Férias regulamentares');
    });

    test('tolera campos ausentes', () {
      final a = AusenciaItem.fromJson(<String, dynamic>{});
      expect(a.id, '');
      expect(a.tipo, '');
      expect(a.dias, 0);
      expect(a.observacao, isNull);
    });
  });

  group('TrilhaEventoItem.fromJson (meu histórico)', () {
    test('parseia o evento da trilha', () {
      final e = TrilhaEventoItem.fromJson(<String, dynamic>{
        'instante': '2026-03-02T11:00:00Z',
        'categoria': 'CORRECAO',
        'titulo': 'Correção aprovada',
        'detalhe': 'ok',
      });
      expect(e.instante, '2026-03-02T11:00:00Z');
      expect(e.categoria, 'CORRECAO');
      expect(e.titulo, 'Correção aprovada');
      expect(e.detalhe, 'ok');
    });

    test('tolera campos ausentes', () {
      final e = TrilhaEventoItem.fromJson(<String, dynamic>{});
      expect(e.instante, '');
      expect(e.categoria, '');
      expect(e.titulo, '');
      expect(e.detalhe, '');
    });
  });

  group('GestorResumo.fromJson (app do gestor)', () {
    test('parseia o resumo de gestão', () {
      final g = GestorResumo.fromJson(<String, dynamic>{'souGestor': true, 'pendentes': 3});
      expect(g.souGestor, true);
      expect(g.pendentes, 3);
    });

    test('tolera campos ausentes (não-gestor)', () {
      final g = GestorResumo.fromJson(<String, dynamic>{});
      expect(g.souGestor, false);
      expect(g.pendentes, 0);
    });
  });
}
