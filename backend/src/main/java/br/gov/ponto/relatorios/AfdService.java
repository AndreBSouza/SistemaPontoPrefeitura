package br.gov.ponto.relatorios;

import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.JornadaHorarioRepository;
import br.gov.ponto.jornada.JornadaRepository;
import br.gov.ponto.jornada.domain.Jornada;
import br.gov.ponto.jornada.domain.JornadaHorario;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.relatorios.api.AfdResponse;
import br.gov.ponto.tenant.TenantRepository;
import br.gov.ponto.tenant.domain.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Geracao do AFD (Arquivo-Fonte de Dados) da Portaria MTP 671/2021.
 *
 * <p>Layout de largura fixa, núcleo herdado da Portaria 1510/2009 (lido pelo programa de
 * tratamento do MTP): cabeçalho (tipo 1), marcações (tipo 3) e trailer (tipo 9 com a
 * contagem por tipo). O identificador do trabalhador é o CPF (modernização do REP-P).
 * Acompanha um hash SHA-256 do conteúdo como integridade adicional.</p>
 *
 * <p>Inclui os registros tipo 5 (empregados: CPF + nome). Exige o CNPJ do ente cadastrado (senão
 * recusa a emissão, pois o cabeçalho sairia inválido). A assinatura digital ICP-Brasil é aplicada
 * por {@link AssinaturaService} quando há certificado configurado (bean {@code @Primary}).</p>
 *
 * <p><b>Pendências para homologação:</b> o registro tipo 6 (eventos sensíveis do REP) e a
 * conferência final de posições/tamanhos e da sequência global de NSR contra o Anexo vigente da
 * Portaria 671. O hash SHA-256 é integridade complementar, não substitui a assinatura.</p>
 */
@Service
public class AfdService {

    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("ddMMyyyy").withZone(TempoMunicipal.ZONE);
    private static final DateTimeFormatter HORA =
            DateTimeFormatter.ofPattern("HHmm").withZone(TempoMunicipal.ZONE);

    private final RegistroPontoRepository registroRepository;
    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;
    private final JornadaRepository jornadaRepository;
    private final JornadaHorarioRepository horarioRepository;
    private final TenantRepository tenantRepository;
    private final AssinaturaService assinaturaService;

    public AfdService(RegistroPontoRepository registroRepository,
                      VinculoRepository vinculoRepository,
                      ServidorRepository servidorRepository,
                      JornadaRepository jornadaRepository,
                      JornadaHorarioRepository horarioRepository,
                      TenantRepository tenantRepository,
                      AssinaturaService assinaturaService) {
        this.registroRepository = registroRepository;
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
        this.jornadaRepository = jornadaRepository;
        this.horarioRepository = horarioRepository;
        this.tenantRepository = tenantRepository;
        this.assinaturaService = assinaturaService;
    }

    @Transactional(readOnly = true)
    public AfdResponse gerar(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente inexistente"));
        // Sem CNPJ o cabeçalho do AFD sai zerado e o arquivo é rejeitado. Exige o cadastro antes.
        if (tenant.getCnpj() == null || tenant.getCnpj().isBlank()) {
            throw new IllegalStateException(
                    "Informe o CNPJ do ente antes de emitir o AFD (Identidade visual → CNPJ do ente).");
        }
        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);
        Instant inicio = periodo[0];
        Instant fim = periodo[1];

        List<Servidor> servidores = servidorRepository.findByTenantId(tenantId);
        Map<UUID, String> cpfPorServidor = new HashMap<>();
        for (Servidor s : servidores) {
            cpfPorServidor.put(s.getId(), s.getCpf());
        }
        Map<UUID, UUID> servidorPorVinculo = new HashMap<>();
        for (Vinculo v : vinculoRepository.findByTenantId(tenantId)) {
            servidorPorVinculo.put(v.getId(), v.getServidorId());
        }

        List<RegistroPonto> registros = registroRepository
                .findByTenantIdAndDataHoraServidorBetweenOrderByNsr(tenantId, inicio, fim);

        Instant agora = Instant.now();
        StringBuilder sb = new StringBuilder();

        // Cabeçalho (tipo 1): NSR(9) + "1" + idEmpregador(1) + CNPJ(14) + CEI(12)
        //                     + razão social(150) + identificador do REP(17)
        //                     + dt inicial(8) + dt final(8) + dt geração(8) + hr geração(4)
        sb.append(num(0, 9)).append('1')
                .append('1')
                .append(num(soDigitos(tenant.getCnpj()), 14))
                .append(texto("", 12))
                .append(texto(tenant.getNome(), 150))
                .append(texto(tenant.getSlug(), 17))
                .append(DATA.format(inicio)).append(DATA.format(fim))
                .append(DATA.format(agora)).append(HORA.format(agora))
                .append('\n');

        // Empregados (tipo 5): NSR(9) + "5" + dataGrav(8) + horaGrav(4) + operação(1: I) + CPF(12) + nome(52).
        // Identifica os servidores do ente no arquivo. NSR próprio deste bloco no AFD gerado; conferir
        // a sequência global e as posições contra o Anexo vigente antes de homologar.
        long nsrCadastro = 0;
        for (Servidor s : servidores) {
            sb.append(num(++nsrCadastro, 9)).append('5')
                    .append(DATA.format(agora)).append(HORA.format(agora))
                    .append('I')
                    .append(num(soDigitos(s.getCpf()), 12))
                    .append(texto(s.getNome(), 52))
                    .append('\n');
        }

        // Marcações (tipo 3): NSR(9) + "3" + data(8) + hora(4) + CPF(12)
        for (RegistroPonto r : registros) {
            String cpf = cpfPorServidor.getOrDefault(servidorPorVinculo.get(r.getVinculoId()), "");
            sb.append(num(r.getNsr(), 9)).append('3')
                    .append(DATA.format(r.getDataHoraServidor()))
                    .append(HORA.format(r.getDataHoraServidor()))
                    .append(num(soDigitos(cpf), 12))
                    .append('\n');
        }

        // Trailer (tipo 9): NSR(9 noves) + "9" + qtd tipo2(9) + tipo3(9) + tipo4(9) + tipo5(9) + "9"
        sb.append("999999999").append('9')
                .append(num(0, 9))
                .append(num(registros.size(), 9))
                .append(num(0, 9))
                .append(num(servidores.size(), 9))
                .append('9')
                .append('\n');

        String conteudo = sb.toString();
        String assinatura = assinaturaService.assinar(conteudo.getBytes(StandardCharsets.UTF_8)).orElse(null);
        return new AfdResponse(competencia.toString(), registros.size(), sha256(conteudo), conteudo, assinatura);
    }

    /**
     * Gera o AEJ (Arquivo Eletronico de Jornada) representativo com as jornadas e
     * horarios do ente + hash de integridade. Validar contra o leiaute oficial antes da producao.
     */
    @Transactional(readOnly = true)
    public AfdResponse gerarAej(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        StringBuilder sb = new StringBuilder();
        sb.append("1").append(tenantId).append(competencia).append("\n"); // cabecalho (tipo 1)
        int total = 0;
        for (Jornada jornada : jornadaRepository.findByTenantId(tenantId)) {
            for (JornadaHorario h : horarioRepository.findByJornadaIdAndTenantId(jornada.getId(), tenantId)) {
                sb.append("2") // jornada contratual (tipo 2)
                        .append(String.format("%-30.30s", jornada.getNome()))
                        .append(jornada.getTipo())
                        .append(";dia=").append(h.getDiaSemana())
                        .append(";").append(h.getHoraEntrada()).append("-").append(h.getHoraSaida())
                        .append("\n");
                total++;
            }
        }
        sb.append("9").append(String.format("%09d", total)).append("\n"); // trailer (tipo 9)
        String conteudo = sb.toString();
        String assinatura = assinaturaService.assinar(conteudo.getBytes(StandardCharsets.UTF_8)).orElse(null);
        return new AfdResponse(competencia.toString(), total, sha256(conteudo), conteudo, assinatura);
    }

    /** Texto à esquerda, truncado/preenchido com espaços à direita até {@code tam}. */
    private static String texto(String valor, int tam) {
        String s = valor == null ? "" : valor;
        return s.length() >= tam ? s.substring(0, tam) : s + " ".repeat(tam - s.length());
    }

    /** Numérico com zeros à esquerda até {@code tam}. */
    private static String num(long valor, int tam) {
        return String.format("%0" + tam + "d", valor);
    }

    private static long soDigitos(String valor) {
        if (valor == null) {
            return 0L;
        }
        String d = valor.replaceAll("\\D", "");
        return d.isEmpty() ? 0L : Long.parseLong(d);
    }

    private String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
