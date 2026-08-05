package br.gov.ponto.auditoria;

import br.gov.ponto.auditoria.domain.AuditoriaEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditoriaRepository extends JpaRepository<AuditoriaEvento, UUID> {

    List<AuditoriaEvento> findByTenantIdOrderByOcorridoEmDesc(UUID tenantId);

    List<AuditoriaEvento> findByTenantIdAndEntidadeAndEntidadeIdOrderByOcorridoEmDesc(
            UUID tenantId, String entidade, String entidadeId);
}
