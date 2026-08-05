package br.gov.ponto.relatorios;

import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.EventoRepRepository;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.EventoRep;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.relatorios.api.AfdResponse;
import br.gov.ponto.relatorios.rep.CampoLeiaute;
import br.gov.ponto.relatorios.rep.ConfigRep;
import br.gov.ponto.relatorios.rep.MontadorAfd;
import br.gov.ponto.tenant.TenantRepository;
import br.gov.ponto.tenant.domain.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Geração do <b>AFD (Arquivo-Fonte de Dados)</b> do REP-P, conforme o leiaute VIGENTE (versão
 * "004", publicado no portal gov.br por força do art. 81 da Portaria MTP 671/2021).
 *
 * <p>Este serviço só busca e ordena os dados; a formatação byte a byte fica no
 * {@link MontadorAfd} (POJO puro, coberto por teste de posição de campo).</p>
 *
 * <p>Os registros saem <b>ordenados por NSR</b> (exigência do leiaute), misturando as operações do
 * ARP (tipos "5" e "6") com as marcações de ponto (tipo <b>"7"</b>, específico do REP-P — o tipo
 * "3" é de REP-C/REP-A).</p>
 */
@Service
public class AfdService {

    private final RegistroPontoRepository registroRepository;
    private final EventoRepRepository eventoRepRepository;
    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;
    private final TenantRepository tenantRepository;
    private final AssinaturaService assinaturaService;
    private final ConfigRep configRep;

    public AfdService(RegistroPontoRepository registroRepository,
                      EventoRepRepository eventoRepRepository,
                      VinculoRepository vinculoRepository,
                      ServidorRepository servidorRepository,
                      TenantRepository tenantRepository,
                      AssinaturaService assinaturaService,
                      ConfigRep configRep) {
        this.registroRepository = registroRepository;
        this.eventoRepRepository = eventoRepRepository;
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
        this.tenantRepository = tenantRepository;
        this.assinaturaService = assinaturaService;
        this.configRep = configRep;
    }

    @Transactional(readOnly = true)
    public AfdResponse gerar(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente inexistente"));
        // Sem CNPJ o cabeçalho sai inválido; sem INPI o REP-P não pode se identificar (art. 91).
        if (tenant.getCnpj() == null || tenant.getCnpj().isBlank()) {
            throw new IllegalStateException(
                    "Informe o CNPJ do ente antes de emitir o AFD (Identidade visual → CNPJ do ente).");
        }
        String inpi = configRep.exigirInpi();

        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);
        Instant inicio = periodo[0];
        Instant fim = periodo[1];

        MontadorAfd montador = new MontadorAfd();
        montador.cabecalho(tenant.getCnpj(), true, null, tenant.getNome(), inpi,
                configRep.desenvolvedorCnpj(), true,
                competencia.atDay(1), competencia.atEndOfMonth(), Instant.now());

        Map<UUID, String> cpfPorVinculo = cpfPorVinculo(tenantId);

        // Ordenar por NSR é exigência do leiaute (item 4): as operações do ARP e as marcações
        // compartilham a mesma sequência, então precisam ser intercaladas na ordem correta.
        List<Object> emOrdem = new ArrayList<>();
        emOrdem.addAll(eventoRepRepository.findByTenantIdAndDataHoraBetweenOrderByNsr(tenantId, inicio, fim));
        List<RegistroPonto> marcacoes =
                registroRepository.findByTenantIdAndDataHoraServidorBetweenOrderByNsr(tenantId, inicio, fim);
        emOrdem.addAll(marcacoes);
        emOrdem.sort((a, b) -> Long.compare(nsrDe(a), nsrDe(b)));

        for (Object item : emOrdem) {
            if (item instanceof EventoRep e) {
                if (e.getTipoRegistro() == EventoRep.TIPO_EMPREGADO) {
                    montador.empregado(e.getNsr(), e.getDataHora(), e.getOperacao().charAt(0),
                            e.getCpf(), e.getNome(), e.getCpfResponsavel());
                } else {
                    montador.eventoSensivel(e.getNsr(), e.getDataHora(), eventoDe(e.getCodigoEvento()));
                }
            } else {
                RegistroPonto r = (RegistroPonto) item;
                montador.marcacao(r.getNsr(), r.getDataHoraServidor(),
                        cpfPorVinculo.getOrDefault(r.getVinculoId(), ""),
                        r.getCriadoEm() != null ? r.getCriadoEm() : r.getDataHoraServidor(),
                        coletorDe(r.getOrigem()), r.isOffline());
            }
        }

        String conteudo = montador.finalizar();
        String assinatura = assinaturaService.assinar(conteudo.getBytes(CampoLeiaute.CHARSET)).orElse(null);
        return new AfdResponse(competencia.toString(), montador.quantidadeDeMarcacoes(),
                sha256(conteudo), conteudo, assinatura);
    }

    /** Nome do arquivo exigido pelo leiaute: AFD + nº INPI + CNPJ do empregador + REP_P. */
    @Transactional(readOnly = true)
    public String nomeDoArquivo() {
        Tenant tenant = tenantRepository.findById(TenantContext.requireCurrent())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente inexistente"));
        return MontadorAfd.nomeDoArquivo(configRep.exigirInpi(), tenant.getCnpj());
    }

    private Map<UUID, String> cpfPorVinculo(UUID tenantId) {
        Map<UUID, String> cpfPorServidor = new HashMap<>();
        for (Servidor s : servidorRepository.findByTenantId(tenantId)) {
            cpfPorServidor.put(s.getId(), s.getCpf());
        }
        Map<UUID, String> porVinculo = new HashMap<>();
        for (Vinculo v : vinculoRepository.findByTenantId(tenantId)) {
            porVinculo.put(v.getId(), cpfPorServidor.getOrDefault(v.getServidorId(), ""));
        }
        return porVinculo;
    }

    private static long nsrDe(Object item) {
        return item instanceof EventoRep e ? e.getNsr() : ((RegistroPonto) item).getNsr();
    }

    /** Identificador do coletor (campo 6 do registro tipo 7) a partir do canal de origem. */
    private static MontadorAfd.Coletor coletorDe(OrigemRegistro origem) {
        return switch (origem) {
            case MOBILE -> MontadorAfd.Coletor.APLICATIVO_MOBILE;
            case WEB -> MontadorAfd.Coletor.BROWSER;
            case TOTEM -> MontadorAfd.Coletor.DISPOSITIVO_ELETRONICO;
            // Correção aprovada: não houve coleta por dispositivo. No AEJ isso fica explícito
            // como marcação incluída manualmente (fonteMarc "I").
            case AJUSTE -> MontadorAfd.Coletor.OUTRO;
        };
    }

    private static MontadorAfd.EventoSensivel eventoDe(String codigo) {
        return switch (codigo) {
            case "02" -> MontadorAfd.EventoSensivel.RETORNO_DE_ENERGIA;
            case "07" -> MontadorAfd.EventoSensivel.DISPONIBILIDADE_DE_SERVICO;
            default -> MontadorAfd.EventoSensivel.INDISPONIBILIDADE_DE_SERVICO;
        };
    }

    /** Hash de integridade do arquivo inteiro (complementar; não substitui a assinatura). */
    private String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(texto.getBytes(CampoLeiaute.CHARSET)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
