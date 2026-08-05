package br.gov.ponto.cadastro;

import br.gov.ponto.auditoria.AuditoriaService;
import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.RegrasPonto;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.JornadaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LotacaoService {

    private final LotacaoRepository lotacaoRepository;
    private final JornadaRepository jornadaRepository;
    private final AuditoriaService auditoriaService;

    public LotacaoService(LotacaoRepository lotacaoRepository, JornadaRepository jornadaRepository,
                          AuditoriaService auditoriaService) {
        this.lotacaoRepository = lotacaoRepository;
        this.jornadaRepository = jornadaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public Lotacao criar(String nome, String sigla) {
        UUID tenantId = TenantContext.requireCurrent();
        // Sigla única por ente (a importação por órgão resolve a lotação pela sigla).
        if (sigla != null && !sigla.isBlank()
                && lotacaoRepository.findByTenantIdAndSigla(tenantId, sigla).isPresent()) {
            throw new ConflitoException("Já existe órgão com a sigla \"" + sigla + "\" neste ente");
        }
        return lotacaoRepository.save(new Lotacao(tenantId, nome, sigla));
    }

    @Transactional
    public void definirChefia(UUID lotacaoId, UUID chefiaServidorId) {
        Lotacao lotacao = exigir(lotacaoId);
        UUID anterior = lotacao.getChefiaServidorId();
        lotacao.definirChefia(chefiaServidorId);
        lotacaoRepository.save(lotacao);
        auditoriaService.registrar("CHEFIA_ALTERADA", "lotacao", lotacaoId.toString(),
                "antes=" + anterior + " depois=" + chefiaServidorId);
    }

    /** Define as regras de ponto proprias do orgao (jornada padrao validada no ente). */
    @Transactional
    public Lotacao definirRegras(UUID lotacaoId, RegrasPonto regras) {
        UUID tenantId = TenantContext.requireCurrent();
        Lotacao lotacao = exigir(lotacaoId);
        if (regras.getJornadaPadraoId() != null
                && !jornadaRepository.existsByIdAndTenantId(regras.getJornadaPadraoId(), tenantId)) {
            throw new IllegalArgumentException("Jornada padrao inexistente no ente");
        }
        String antes = resumoRegras(lotacao.getRegras());
        lotacao.definirRegras(regras);
        Lotacao salva = lotacaoRepository.save(lotacao);
        // Trilha completa (12.6.16): registra a config das regras antes e depois da alteração.
        auditoriaService.registrar("REGRAS_ORGAO", "lotacao", lotacaoId.toString(),
                "antes=[" + antes + "] depois=[" + resumoRegras(regras) + "]");
        return salva;
    }

    /** Resumo das regras para a trilha de auditoria (antes/depois legível). */
    private String resumoRegras(RegrasPonto r) {
        if (r == null) {
            return "vazio";
        }
        return "tol=" + r.getToleranciaMinutos()
                + ", banco=" + r.getBancoHorasHabilitado()
                + ", teto=" + r.getTetoBancoHorasMinutos()
                + ", geofenceRaio=" + r.getGeofenceRaioMetros()
                + ", verificacao=" + r.getVerificacaoObrigatoria()
                + ", adaptacaoAte=" + r.getAdaptacaoAte();
    }

    @Transactional(readOnly = true)
    public List<Lotacao> listar() {
        return lotacaoRepository.findByTenantId(TenantContext.requireCurrent());
    }

    private Lotacao exigir(UUID lotacaoId) {
        return lotacaoRepository.findByIdAndTenantId(lotacaoId, TenantContext.requireCurrent())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lotacao inexistente"));
    }
}
