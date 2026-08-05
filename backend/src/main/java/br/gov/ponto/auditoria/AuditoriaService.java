package br.gov.ponto.auditoria;

import br.gov.ponto.auditoria.domain.AuditoriaEvento;
import br.gov.ponto.common.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Registra e consulta a trilha de auditoria imutavel. */
@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional
    public void registrar(String acao, String entidade, String entidadeId, String detalhe) {
        UUID tenantId = TenantContext.requireCurrent();
        auditoriaRepository.save(new AuditoriaEvento(tenantId, acao, entidade, entidadeId, ator(), detalhe));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaEvento> listar() {
        return auditoriaRepository.findByTenantIdOrderByOcorridoEmDesc(TenantContext.requireCurrent());
    }

    @Transactional(readOnly = true)
    public List<AuditoriaEvento> listarPorEntidade(String entidade, String entidadeId) {
        return auditoriaRepository.findByTenantIdAndEntidadeAndEntidadeIdOrderByOcorridoEmDesc(
                TenantContext.requireCurrent(), entidade, entidadeId);
    }

    private String ator() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            String usuario = jwt.getToken().getClaimAsString("preferred_username");
            return usuario != null ? usuario : jwt.getToken().getSubject();
        }
        return auth != null ? auth.getName() : "sistema";
    }
}
