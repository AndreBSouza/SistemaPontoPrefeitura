package br.gov.ponto.apuracao;

import br.gov.ponto.apuracao.domain.ApuracaoDia;
import br.gov.ponto.apuracao.domain.CalculadoraApuracaoDia;
import br.gov.ponto.apuracao.domain.Justificativa;
import br.gov.ponto.apuracao.domain.Marcacao;
import br.gov.ponto.apuracao.domain.Ocorrencia;
import br.gov.ponto.apuracao.domain.ResultadoApuracao;
import br.gov.ponto.apuracao.domain.StatusJustificativa;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.ausencia.AusenciaService;
import br.gov.ponto.cadastro.RegrasPontoService;
import br.gov.ponto.cadastro.domain.RegrasPonto;
import br.gov.ponto.calendario.CalendarioService;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.EscalaRepository;
import br.gov.ponto.jornada.JornadaHorarioRepository;
import br.gov.ponto.jornada.JornadaRepository;
import br.gov.ponto.jornada.domain.Escala;
import br.gov.ponto.jornada.domain.Jornada;
import br.gov.ponto.jornada.domain.JornadaHorario;
import br.gov.ponto.jornada.domain.TipoJornada;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.RegistroPonto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestra a apuracao diaria: carrega escala/jornada/horario/registros/justificativas (I/O)
 * e delega a aritmetica a {@link CalculadoraApuracaoDia}. As justificativas aprovadas que
 * cobrem o dia neutralizam as ocorrencias correspondentes (regra no enum TipoJustificativa).
 */
@Service
public class ApuracaoService {

    private final EscalaRepository escalaRepository;
    private final JornadaRepository jornadaRepository;
    private final JornadaHorarioRepository horarioRepository;
    private final RegistroPontoRepository registroRepository;
    private final JustificativaRepository justificativaRepository;
    private final RegrasPontoService regrasPontoService;
    private final CalendarioService calendarioService;
    private final AusenciaService ausenciaService;
    private final CalculadoraApuracaoDia calculadora = new CalculadoraApuracaoDia();

    public ApuracaoService(EscalaRepository escalaRepository,
                           JornadaRepository jornadaRepository,
                           JornadaHorarioRepository horarioRepository,
                           RegistroPontoRepository registroRepository,
                           JustificativaRepository justificativaRepository,
                           RegrasPontoService regrasPontoService,
                           CalendarioService calendarioService,
                           AusenciaService ausenciaService) {
        this.escalaRepository = escalaRepository;
        this.jornadaRepository = jornadaRepository;
        this.horarioRepository = horarioRepository;
        this.registroRepository = registroRepository;
        this.justificativaRepository = justificativaRepository;
        this.regrasPontoService = regrasPontoService;
        this.calendarioService = calendarioService;
        this.ausenciaService = ausenciaService;
    }

    @Transactional(readOnly = true)
    public ApuracaoDia apurarDia(UUID vinculoId, LocalDate data) {
        UUID tenantId = TenantContext.requireCurrent();

        Optional<Escala> escala = escalaRepository.findByVinculoIdAndTenantId(vinculoId, tenantId).stream()
                .filter(e -> e.vigenteEm(data))
                .findFirst();

        Instant[] dia = TempoMunicipal.intervaloDoDia(data);
        List<RegistroPonto> registrosDoDia = registroRepository
                .findByVinculoIdAndTenantIdAndDataHoraServidorBetweenOrderByDataHoraServidor(
                        vinculoId, tenantId, dia[0], dia[1]);
        List<Marcacao> marcacoes = registrosDoDia.stream()
                .map(r -> new Marcacao(minuto(r.getDataHoraServidor()
                        .atZone(TempoMunicipal.ZONE).toLocalTime()), r.getTipo()))
                .toList();

        Optional<Jornada> jornada = escala
                .flatMap(e -> jornadaRepository.findByIdAndTenantId(e.getJornadaId(), tenantId));
        Optional<JornadaHorario> horario = escala
                .flatMap(e -> horarioEsperado(e, jornada, data, tenantId));

        // Dia não útil quando há feriado/ponto facultativo/abono coletivo (calendário) OU o
        // servidor está em férias/licença programada. Sem horário esperado → sem falta; o
        // trabalho eventual do dia vira hora extra.
        boolean diaNaoUtil = calendarioService.diaNaoUtilParaVinculo(vinculoId, data)
                || ausenciaService.estaAusente(vinculoId, data);
        Integer entradaEsp = diaNaoUtil ? null
                : horario.map(h -> minuto(h.getHoraEntrada())).orElse(null);
        Integer saidaEsp = diaNaoUtil ? null
                : horario.map(h -> minuto(h.getHoraSaida())).orElse(null);
        // Tolerancia: override do orgao tem precedencia sobre a da jornada.
        RegrasPonto regrasOrgao = regrasPontoService.regrasDoOrgaoDoVinculo(vinculoId);
        int tolerancia = regrasOrgao.getToleranciaMinutos() != null
                ? regrasOrgao.getToleranciaMinutos()
                : jornada.map(Jornada::getToleranciaMin).orElse(0);
        int intervaloMin = jornada.map(Jornada::getIntervaloMin).orElse(0);

        ResultadoApuracao resultado = calculadora.calcular(marcacoes, entradaEsp, saidaEsp, tolerancia, intervaloMin);

        List<Ocorrencia> ocorrencias = new ArrayList<>(resultado.ocorrencias());
        // Geofence não penaliza o servidor: "fora da área" é só verificação do administrador
        // (fica no registro, com a localização). Não vira ocorrência na apuração.
        boolean justificado = aplicarJustificativas(vinculoId, tenantId, data, ocorrencias);

        // Modo adaptacao do orgao: no periodo inicial so registra, sem descontar/penalizar.
        // Suprime as ocorrencias que penalizam o servidor (atraso/falta/saida antecipada);
        // creditos a favor (hora extra/adicional) seguem valendo. Reflete no banco de horas,
        // no espelho e nos relatorios, que consomem esta apuracao.
        boolean emAdaptacao = regrasOrgao.emAdaptacao(data);
        if (emAdaptacao) {
            ocorrencias.removeIf(o -> o.tipo().penalizaServidor());
        }

        return new ApuracaoDia(vinculoId, data, resultado.minutosTrabalhados(),
                resultado.minutosEsperados(), resultado.diaUtil(), justificado, emAdaptacao, ocorrencias);
    }

    /** Remove ocorrencias cobertas por justificativas aprovadas no dia (regra no enum). */
    private boolean aplicarJustificativas(UUID vinculoId, UUID tenantId, LocalDate data,
                                          List<Ocorrencia> ocorrencias) {
        List<Justificativa> abonos = justificativaRepository
                .findByVinculoIdAndTenantIdAndStatusAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
                        vinculoId, tenantId, StatusJustificativa.APROVADA, data, data);
        if (abonos.isEmpty()) {
            return false;
        }
        ocorrencias.removeIf(o -> abonos.stream().anyMatch(j -> j.getTipo().neutraliza(o.tipo())));
        return true;
    }

    private int minuto(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    /**
     * Horário esperado do vínculo no dia. Escala 12x36 ({@link TipoJornada#ESCALA_12X36}) trabalha
     * em dias alternados — 12h de trabalho + 36h de descanso = ciclo de 48h — a partir da data de
     * início da escala (âncora): trabalha quando a diferença de dias é par. Nos dias de descanso
     * não há horário esperado (sem falta; o trabalho eventual vira hora extra). Demais jornadas
     * usam o horário do dia da semana.
     */
    private Optional<JornadaHorario> horarioEsperado(Escala escala, Optional<Jornada> jornada,
                                                     LocalDate data, UUID tenantId) {
        List<JornadaHorario> horarios = horarioRepository
                .findByJornadaIdAndTenantId(escala.getJornadaId(), tenantId);
        boolean rotativa12x36 = jornada.map(j -> j.getTipo() == TipoJornada.ESCALA_12X36).orElse(false);
        if (rotativa12x36) {
            long delta = data.toEpochDay() - escala.getDataInicio().toEpochDay();
            boolean diaDeTrabalho = delta >= 0 && delta % 2 == 0;
            // 12x36 usa um único turno; se houver mais de um horário, escolhe de forma determinística.
            return diaDeTrabalho
                    ? horarios.stream().min(java.util.Comparator.comparingInt(JornadaHorario::getDiaSemana))
                    : Optional.empty();
        }
        return horarios.stream()
                .filter(h -> h.getDiaSemana() == data.getDayOfWeek().getValue())
                .findFirst();
    }
}
