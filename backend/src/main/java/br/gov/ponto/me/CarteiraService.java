package br.gov.ponto.me;

import br.gov.ponto.cadastro.LotacaoRepository;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.me.api.CarteiraResponse;
import br.gov.ponto.tenant.BrandingService;
import br.gov.ponto.tenant.api.BrandingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Compõe a carteira funcional digital do servidor (12.3.8): dados do vínculo + ente. */
@Service
public class CarteiraService {

    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;
    private final LotacaoRepository lotacaoRepository;
    private final BrandingService brandingService;

    public CarteiraService(VinculoRepository vinculoRepository, ServidorRepository servidorRepository,
                           LotacaoRepository lotacaoRepository, BrandingService brandingService) {
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
        this.lotacaoRepository = lotacaoRepository;
        this.brandingService = brandingService;
    }

    @Transactional(readOnly = true)
    public CarteiraResponse carteira(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        Vinculo v = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vinculo inexistente"));
        Servidor s = servidorRepository.findByIdAndTenantId(v.getServidorId(), tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Servidor inexistente"));
        String orgao = v.getLotacaoId() == null ? null
                : lotacaoRepository.findByIdAndTenantId(v.getLotacaoId(), tenantId)
                .map(Lotacao::getNome).orElse(null);
        BrandingResponse b = brandingService.obter();
        return new CarteiraResponse(s.getNome(), s.getCpf(), v.getMatricula(), v.getCargo(),
                v.getRegime().name(), orgao, b.nomeEnte(), b.logoUrl(), b.corPrimaria());
    }
}
