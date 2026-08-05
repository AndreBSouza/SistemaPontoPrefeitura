package br.gov.ponto.ativacao;

import br.gov.ponto.ativacao.api.AtivacaoResponse;
import br.gov.ponto.ativacao.api.GerarCodigoResponse;
import br.gov.ponto.ativacao.domain.CodigoAtivacao;
import br.gov.ponto.ativacao.domain.Dispositivo;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.tenant.BrandingService;
import br.gov.ponto.tenant.api.BrandingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ativacao de aparelho por codigo do RH. O codigo (uso unico, com validade) e trocado
 * por um token de dispositivo de longa duracao — o servidor faz isso uma vez por aparelho
 * e nao precisa relogar. O RH lista e revoga dispositivos.
 */
@Service
public class AtivacaoService {

    private static final int VALIDADE_HORAS_PADRAO = 24;

    private final CodigoAtivacaoRepository codigoRepository;
    private final DispositivoRepository dispositivoRepository;
    private final VinculoRepository vinculoRepository;
    private final CacheDispositivo cacheDispositivo;
    private final BrandingService brandingService;

    public AtivacaoService(CodigoAtivacaoRepository codigoRepository,
                           DispositivoRepository dispositivoRepository,
                           VinculoRepository vinculoRepository,
                           CacheDispositivo cacheDispositivo,
                           BrandingService brandingService) {
        this.codigoRepository = codigoRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.vinculoRepository = vinculoRepository;
        this.cacheDispositivo = cacheDispositivo;
        this.brandingService = brandingService;
    }

    /**
     * Identidade visual do ente resolvida pelo CÓDIGO de ativação (12.2.2) — para a tela de
     * ativação do app mostrar a marca da prefeitura antes mesmo de ativar. Pré-auth: o código
     * resolve o ente (sem tenant no contexto).
     */
    public BrandingResponse brandingDoCodigo(String codigo) {
        CodigoAtivacao ca = codigoRepository.findByCodigoHash(Tokens.hash(Tokens.normalizarCodigo(codigo)))
                .orElseThrow(() -> new IllegalArgumentException("Codigo de ativacao invalido"));
        try {
            TenantContext.set(ca.getTenantId().toString());
            return brandingService.obter();
        } finally {
            TenantContext.clear();
        }
    }

    /** RH gera um codigo de ativacao para um vinculo do seu ente. */
    @Transactional
    public GerarCodigoResponse gerar(UUID vinculoId, Integer validadeHoras) {
        UUID tenantId = TenantContext.requireCurrent();
        if (!vinculoRepository.existsByIdAndTenantId(vinculoId, tenantId)) {
            throw new RecursoNaoEncontradoException("Vinculo inexistente no ente");
        }
        String codigo = Tokens.gerarCodigoLegivel();
        Instant expira = Instant.now().plus(Duration.ofHours(
                validadeHoras != null && validadeHoras > 0 ? validadeHoras : VALIDADE_HORAS_PADRAO));
        codigoRepository.save(new CodigoAtivacao(tenantId, vinculoId,
                Tokens.hash(Tokens.normalizarCodigo(codigo)), expira));
        return new GerarCodigoResponse(codigo, vinculoId, expira);
    }

    /** App troca o codigo por um token de dispositivo (sem tenant no contexto — o codigo resolve o ente). */
    @Transactional
    public AtivacaoResponse ativar(String codigo, String nomeDispositivo) {
        CodigoAtivacao ca = codigoRepository.findByCodigoHash(Tokens.hash(Tokens.normalizarCodigo(codigo)))
                .orElseThrow(() -> new IllegalArgumentException("Codigo de ativacao invalido"));
        if (!ca.valido(Instant.now())) {
            throw new ConflitoException("Codigo de ativacao expirado ou ja utilizado");
        }
        ca.marcarUsado();
        codigoRepository.save(ca);

        String token = Tokens.gerarToken();
        String nome = (nomeDispositivo == null || nomeDispositivo.isBlank())
                ? "Aparelho do servidor" : nomeDispositivo.trim();
        Dispositivo d = dispositivoRepository.save(
                new Dispositivo(ca.getTenantId(), ca.getVinculoId(), nome, Tokens.hash(token)));
        return new AtivacaoResponse(token, ca.getTenantId(), ca.getVinculoId(), d.getId(), d.getNome());
    }

    @Transactional(readOnly = true)
    public List<Dispositivo> listarDispositivos(UUID vinculoId) {
        return dispositivoRepository.findByTenantIdAndVinculoIdOrderByCriadoEm(
                TenantContext.requireCurrent(), vinculoId);
    }

    @Transactional
    public void revogar(UUID dispositivoId) {
        Dispositivo d = dispositivoRepository.findByIdAndTenantId(dispositivoId, TenantContext.requireCurrent())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dispositivo inexistente"));
        d.revogar();
        dispositivoRepository.save(d);
        cacheDispositivo.invalidar(d.getTokenHash());
    }

    /**
     * Autenticacao por token de dispositivo (cache de TTL curto; cai no banco só no miss).
     * Devolve o principal (identificadores), não a entidade — é o que a cadeia de segurança usa
     * e o que o cache distribuído consegue serializar com segurança.
     */
    public Optional<DispositivoPrincipal> autenticarPorToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String hash = Tokens.hash(token.trim());
        return cacheDispositivo.obter(hash, () -> dispositivoRepository.findByTokenHashAndAtivoTrue(hash)
                .map(d -> new DispositivoPrincipal(d.getTenantId(), d.getVinculoId(), d.getId())));
    }
}
