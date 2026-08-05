package br.gov.ponto.tenant;

import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.tenant.domain.Branding;
import br.gov.ponto.tenant.domain.Tenant;
import br.gov.ponto.tenant.domain.TenantLogo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Logo do ente armazenada no banco (alternativa ao S3, 12.2.1). Ao salvar, grava os bytes e
 * aponta o {@code Branding.logoUrl} para o endpoint público de serviço (por slug), de modo que o
 * white-label existente passa a exibir o logo sem nenhuma outra mudança.
 */
@Service
public class LogoService {

    private final TenantLogoRepository logoRepository;
    private final TenantRepository tenantRepository;

    public LogoService(TenantLogoRepository logoRepository, TenantRepository tenantRepository) {
        this.logoRepository = logoRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public void salvar(byte[] conteudo, String contentType) {
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("O arquivo deve ser uma imagem");
        }
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("Imagem vazia");
        }
        UUID tenantId = TenantContext.requireCurrent();
        Tenant t = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente inexistente"));

        TenantLogo logo = logoRepository.findById(tenantId).orElse(null);
        if (logo == null) {
            logoRepository.save(new TenantLogo(tenantId, contentType, conteudo));
        } else {
            logo.atualizar(contentType, conteudo);
            logoRepository.save(logo);
        }

        Branding atual = t.getBranding() != null ? t.getBranding() : Branding.vazia();
        t.setBranding(atual.comLogoUrl("/api/publico/branding/" + t.getSlug() + "/logo"));
        tenantRepository.save(t);
    }

    /**
     * Logo do ente pelo slug público. NÃO é @Transactional de propósito: resolve o tenant na
     * tabela global e busca os bytes já no contexto do tenant (RLS) — uma transação externa
     * fixaria a conexão antes de o contexto ser definido. Espelha {@code TransparenciaService}.
     */
    public TenantLogo buscarPorSlug(String slug) {
        Tenant t = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente não encontrado"));
        try {
            TenantContext.set(t.getId().toString());
            return logoRepository.findById(t.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Ente sem logo"));
        } finally {
            TenantContext.clear();
        }
    }
}
