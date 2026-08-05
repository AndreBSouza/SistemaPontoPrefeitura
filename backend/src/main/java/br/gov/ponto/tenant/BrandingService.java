package br.gov.ponto.tenant;

import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.tenant.api.BrandingResponse;
import br.gov.ponto.tenant.api.DefinirBrandingRequest;
import br.gov.ponto.tenant.api.EntePublicoResponse;
import br.gov.ponto.tenant.domain.Branding;
import br.gov.ponto.tenant.domain.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Identidade visual (white-label) do ente atual — lida pelo app e pelo painel. */
@Service
public class BrandingService {

    private final TenantRepository tenantRepository;

    public BrandingService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public BrandingResponse obter() {
        Tenant t = tenantAtual();
        Branding b = t.getBranding() != null ? t.getBranding() : Branding.vazia();
        return new BrandingResponse(t.getNome(), b.nomeAppOu("Ponto Municipal"),
                b.getLogoUrl(), b.corPrimaria(), b.corAcento(), t.getCnpj());
    }

    /**
     * Define o CNPJ do ente atual (14 dígitos), usado no cabeçalho do AFD (Portaria 671).
     * Aceita entrada com máscara; guarda só os dígitos.
     */
    @Transactional
    public void definirCnpj(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("CNPJ obrigatório");
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() != 14) {
            throw new IllegalArgumentException("CNPJ deve ter 14 dígitos");
        }
        Tenant t = tenantAtual();
        t.setCnpj(digitos);
        tenantRepository.save(t);
    }

    @Transactional
    public BrandingResponse definir(DefinirBrandingRequest req) {
        Tenant t = tenantAtual();
        t.setBranding(new Branding(req.nomeApp(), req.logoUrl(), req.corPrimaria(), req.corAcento()));
        tenantRepository.save(t);
        return obter();
    }

    /** Define o subdomínio do ente atual (único). O apontamento DNS é infraestrutura. */
    @Transactional
    public void definirSubdominio(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Subdomínio obrigatório");
        }
        String norm = valor.trim().toLowerCase();
        if (!norm.matches("[a-z0-9-]{2,60}")) {
            throw new IllegalArgumentException("Subdomínio inválido (use a-z, 0-9 e hífen)");
        }
        Tenant t = tenantAtual();
        if (!norm.equals(t.getSubdominio()) && tenantRepository.existsBySubdominio(norm)) {
            throw new ConflitoException("Subdomínio já está em uso por outro ente");
        }
        t.setSubdominio(norm);
        tenantRepository.save(t);
    }

    /** Resolve o ente pelo subdomínio (público) para o app/login se autoconfigurar. */
    @Transactional(readOnly = true)
    public EntePublicoResponse resolverPorSubdominio(String subdominio) {
        Tenant t = tenantRepository.findBySubdominio(subdominio == null ? "" : subdominio.trim().toLowerCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente não encontrado"));
        Branding b = t.getBranding() != null ? t.getBranding() : Branding.vazia();
        return new EntePublicoResponse(t.getSlug(), b.nomeAppOu("Ponto Municipal"),
                b.getLogoUrl(), b.corPrimaria(), b.corAcento());
    }

    private Tenant tenantAtual() {
        return tenantRepository.findById(TenantContext.requireCurrent())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente inexistente"));
    }
}
