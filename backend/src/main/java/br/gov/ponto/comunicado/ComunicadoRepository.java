package br.gov.ponto.comunicado;

import br.gov.ponto.comunicado.domain.Comunicado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComunicadoRepository extends JpaRepository<Comunicado, UUID> {

    List<Comunicado> findByTenantIdOrderByPublicadoEmDesc(UUID tenantId);
}
