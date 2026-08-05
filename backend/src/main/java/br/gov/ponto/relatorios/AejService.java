package br.gov.ponto.relatorios;

import br.gov.ponto.apuracao.ApuracaoService;
import br.gov.ponto.apuracao.domain.ApuracaoDia;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.bancohoras.BancoHorasRepository;
import br.gov.ponto.bancohoras.domain.BancoHorasLancamento;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.EscalaRepository;
import br.gov.ponto.jornada.JornadaHorarioRepository;
import br.gov.ponto.jornada.JornadaRepository;
import br.gov.ponto.jornada.domain.Escala;
import br.gov.ponto.jornada.domain.Jornada;
import br.gov.ponto.jornada.domain.JornadaHorario;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.registro.domain.TipoMarcacao;
import br.gov.ponto.relatorios.api.AfdResponse;
import br.gov.ponto.relatorios.rep.CampoLeiaute;
import br.gov.ponto.relatorios.rep.ConfigRep;
import br.gov.ponto.relatorios.rep.MontadorAej;
import br.gov.ponto.tenant.TenantRepository;
import br.gov.ponto.tenant.domain.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Geração do <b>AEJ (Arquivo Eletrônico de Jornada)</b> — a saída do Programa de Tratamento de
 * Registro de Ponto (art. 83, I), no leiaute VIGENTE <b>versão "002"</b> publicado no portal
 * gov.br (o Anexo VI da Portaria 671 foi revogado pela Portaria MTP 1.486/2022).
 *
 * <p>Enquanto o AFD mostra o que o REP capturou, o AEJ mostra a jornada <b>tratada</b>: além das
 * marcações, traz o horário contratual, as marcações incluídas manualmente (com motivo), as faltas
 * e os movimentos de banco de horas.</p>
 *
 * <p>A formatação fica no {@link MontadorAej} (POJO puro, testado campo a campo).</p>
 */
@Service
public class AejService {

    /** Só existe um REP neste produto (o próprio REP-P), então o identificador no AEJ é fixo. */
    private static final int ID_REP = 1;

    private final RegistroPontoRepository registroRepository;
    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;
    private final TenantRepository tenantRepository;
    private final JornadaRepository jornadaRepository;
    private final JornadaHorarioRepository horarioRepository;
    private final EscalaRepository escalaRepository;
    private final BancoHorasRepository bancoHorasRepository;
    private final ApuracaoService apuracaoService;
    private final AssinaturaService assinaturaService;
    private final ConfigRep configRep;

    public AejService(RegistroPontoRepository registroRepository, VinculoRepository vinculoRepository,
                      ServidorRepository servidorRepository, TenantRepository tenantRepository,
                      JornadaRepository jornadaRepository, JornadaHorarioRepository horarioRepository,
                      EscalaRepository escalaRepository, BancoHorasRepository bancoHorasRepository,
                      ApuracaoService apuracaoService, AssinaturaService assinaturaService,
                      ConfigRep configRep) {
        this.registroRepository = registroRepository;
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
        this.tenantRepository = tenantRepository;
        this.jornadaRepository = jornadaRepository;
        this.horarioRepository = horarioRepository;
        this.escalaRepository = escalaRepository;
        this.bancoHorasRepository = bancoHorasRepository;
        this.apuracaoService = apuracaoService;
        this.assinaturaService = assinaturaService;
        this.configRep = configRep;
    }

    @Transactional(readOnly = true)
    public AfdResponse gerar(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente inexistente"));
        if (tenant.getCnpj() == null || tenant.getCnpj().isBlank()) {
            throw new IllegalStateException(
                    "Informe o CNPJ do ente antes de emitir o AEJ (Identidade visual → CNPJ do ente).");
        }
        String inpi = configRep.exigirInpi();

        LocalDate primeiroDia = competencia.atDay(1);
        LocalDate ultimoDia = competencia.atEndOfMonth();
        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);

        MontadorAej aej = new MontadorAej();
        aej.cabecalho(tenant.getCnpj(), true, null, null, tenant.getNome(),
                primeiroDia, ultimoDia, Instant.now());
        aej.repP(ID_REP, inpi);

        // --- Vínculos (03) + índice para os demais registros -------------------------------
        Map<UUID, String> nomePorServidor = new HashMap<>();
        Map<UUID, String> cpfPorServidor = new HashMap<>();
        Map<UUID, Integer> vinculosPorServidor = new HashMap<>();
        for (Servidor s : servidorRepository.findByTenantId(tenantId)) {
            nomePorServidor.put(s.getId(), s.getNome());
            cpfPorServidor.put(s.getId(), s.getCpf());
        }
        List<Vinculo> vinculos = vinculoRepository.findByTenantId(tenantId);
        for (Vinculo v : vinculos) {
            vinculosPorServidor.merge(v.getServidorId(), 1, Integer::sum);
        }

        Map<UUID, Integer> idPorVinculo = new LinkedHashMap<>();
        int proximoId = 1;
        for (Vinculo v : vinculos) {
            idPorVinculo.put(v.getId(), proximoId);
            aej.vinculo(proximoId, cpfPorServidor.getOrDefault(v.getServidorId(), ""),
                    nomePorServidor.getOrDefault(v.getServidorId(), ""));
            proximoId++;
        }

        // --- Horários contratuais (04) ------------------------------------------------------
        // Um registro por (jornada, dia da semana): é a menor unidade que o leiaute representa,
        // já que durJornada é a duração DIÁRIA e cada dia pode ter horário diferente.
        Map<UUID, Jornada> jornadasPorId = new HashMap<>();
        for (Jornada j : jornadaRepository.findByTenantId(tenantId)) {
            jornadasPorId.put(j.getId(), j);
        }
        Map<String, String> codigoPorJornadaDia = new HashMap<>();
        for (Jornada j : jornadasPorId.values()) {
            for (JornadaHorario h : horarioRepository.findByJornadaIdAndTenantId(j.getId(), tenantId)) {
                String codigo = codigoHorario(j, h.getDiaSemana());
                if (codigoPorJornadaDia.putIfAbsent(j.getId() + "|" + h.getDiaSemana(), codigo) == null) {
                    int duracao = (int) java.time.Duration.between(h.getHoraEntrada(), h.getHoraSaida()).toMinutes();
                    aej.horarioContratual(codigo, duracao, List.of(
                            new MontadorAej.ParEntradaSaida(h.getHoraEntrada(), h.getHoraSaida())));
                }
            }
        }

        // Jornada vigente de cada vínculo, para amarrar a marcação ao horário contratual.
        Map<UUID, UUID> jornadaPorVinculo = new HashMap<>();
        for (Escala e : escalaRepository.findByTenantId(tenantId)) {
            boolean vigente = !e.getDataInicio().isAfter(ultimoDia)
                    && (e.getDataFim() == null || !e.getDataFim().isBefore(primeiroDia));
            if (vigente) {
                jornadaPorVinculo.put(e.getVinculoId(), e.getJornadaId());
            }
        }

        // --- Marcações (05) -----------------------------------------------------------------
        List<RegistroPonto> marcacoes = registroRepository
                .findByTenantIdAndDataHoraServidorBetweenOrderByNsr(tenantId, periodo[0], periodo[1]);
        // Sequência do par entrada/saída, reiniciada a cada dia de cada vínculo.
        Map<String, Integer> sequenciaPorVinculoDia = new HashMap<>();
        for (RegistroPonto r : marcacoes) {
            Integer idVinculo = idPorVinculo.get(r.getVinculoId());
            if (idVinculo == null) {
                continue; // vínculo removido: sem identificador no AEJ
            }
            LocalDate dia = r.getDataHoraServidor().atZone(TempoMunicipal.ZONE).toLocalDate();
            String chave = idVinculo + "|" + dia;
            MontadorAej.TipoMarc tipo = tipoDe(r.getTipo());
            int seq = sequenciaPorVinculoDia.merge(chave,
                    tipo == MontadorAej.TipoMarc.ENTRADA ? 1 : 0, Integer::sum);
            seq = Math.max(seq, 1);

            boolean incluidaManualmente = r.getOrigem() == OrigemRegistro.AJUSTE;
            boolean primeiraEntrada = tipo == MontadorAej.TipoMarc.ENTRADA && seq == 1;
            String codHorario = primeiraEntrada ? codigoDoDia(jornadaPorVinculo.get(r.getVinculoId()),
                    jornadasPorId, dia) : null;

            aej.marcacao(idVinculo, r.getDataHoraServidor(),
                    incluidaManualmente ? null : ID_REP,
                    tipo, seq,
                    incluidaManualmente ? MontadorAej.FonteMarc.INCLUIDA_MANUALMENTE
                            : MontadorAej.FonteMarc.ORIGINAL_DO_REP,
                    codHorario,
                    // "motivo" é obrigatório quando a marcação foi incluída manualmente.
                    incluidaManualmente ? "Correcao de marcacao aprovada" : null);
        }

        // --- Matrícula no eSocial (06): só para quem tem mais de um vínculo -----------------
        for (Vinculo v : vinculos) {
            if (vinculosPorServidor.getOrDefault(v.getServidorId(), 1) > 1) {
                aej.matriculaEsocial(idPorVinculo.get(v.getId()), v.getMatricula());
            }
        }

        // --- Ausências e banco de horas (07) ------------------------------------------------
        for (BancoHorasLancamento l : bancoHorasRepository
                .findByTenantIdAndTipoAndDataBetween(tenantId,
                        br.gov.ponto.bancohoras.domain.TipoLancamento.APURACAO, primeiroDia, ultimoDia)) {
            Integer idVinculo = idPorVinculo.get(l.getVinculoId());
            if (idVinculo == null || l.getMinutos() == 0) {
                continue;
            }
            aej.ausenciaOuBancoDeHoras(idVinculo, MontadorAej.TipoAusencia.BANCO_DE_HORAS, l.getData(),
                    Math.abs(l.getMinutos()),
                    l.getMinutos() > 0 ? MontadorAej.MovimentoBancoHoras.INCLUSAO
                            : MontadorAej.MovimentoBancoHoras.COMPENSACAO);
        }
        // Faltas não justificadas: o fiscal precisa saber por que o dia não tem marcação.
        // A apuração é por dia/vínculo — custo aceitável porque o AEJ é emitido sob demanda.
        for (Vinculo v : vinculos) {
            Integer idVinculo = idPorVinculo.get(v.getId());
            if (idVinculo == null || !jornadaPorVinculo.containsKey(v.getId())) {
                continue;
            }
            for (LocalDate dia = primeiroDia; !dia.isAfter(ultimoDia); dia = dia.plusDays(1)) {
                ApuracaoDia apuracao = apuracaoService.apurarDia(v.getId(), dia);
                boolean falta = apuracao.ocorrencias().stream()
                        .anyMatch(o -> o.tipo() == TipoOcorrencia.FALTA);
                if (falta && !apuracao.justificado()) {
                    aej.ausenciaOuBancoDeHoras(idVinculo,
                            MontadorAej.TipoAusencia.FALTA_NAO_JUSTIFICADA, dia, null, null);
                }
            }
        }

        // --- Identificação do PTRP (08) -----------------------------------------------------
        aej.identificacaoPtrp(configRep.ptrpNome(), configRep.ptrpVersao(), true,
                configRep.desenvolvedorCnpj(), configRep.desenvolvedorNome(),
                configRep.desenvolvedorEmail());

        String conteudo = aej.finalizar();
        String assinatura = assinaturaService.assinar(conteudo.getBytes(CampoLeiaute.CHARSET)).orElse(null);
        return new AfdResponse(competencia.toString(), aej.quantidadeDeMarcacoes(),
                sha256(conteudo), conteudo, assinatura);
    }

    /** Nome do arquivo AEJ, no mesmo padrão do AFD. */
    @Transactional(readOnly = true)
    public String nomeDoArquivo() {
        Tenant tenant = tenantRepository.findById(TenantContext.requireCurrent())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente inexistente"));
        return "AEJ" + configRep.exigirInpi().replaceAll("\\D", "")
                + tenant.getCnpj().replaceAll("\\D", "") + "REP_P.txt";
    }

    private static String codigoHorario(Jornada jornada, int diaSemana) {
        String base = jornada.getNome() == null ? "J" : jornada.getNome().replaceAll("[^A-Za-z0-9]", "");
        return (base.isEmpty() ? "J" : base) + "-" + diaSemana;
    }

    private String codigoDoDia(UUID jornadaId, Map<UUID, Jornada> jornadas, LocalDate dia) {
        Jornada j = jornadaId == null ? null : jornadas.get(jornadaId);
        return j == null ? null : codigoHorario(j, dia.getDayOfWeek().getValue());
    }

    /** INTERVALO_INICIO é uma saída (para o intervalo) e INTERVALO_FIM, o retorno. */
    private static MontadorAej.TipoMarc tipoDe(TipoMarcacao tipo) {
        return switch (tipo) {
            case ENTRADA, INTERVALO_FIM -> MontadorAej.TipoMarc.ENTRADA;
            case SAIDA, INTERVALO_INICIO -> MontadorAej.TipoMarc.SAIDA;
        };
    }

    private String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(texto.getBytes(CampoLeiaute.CHARSET)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
