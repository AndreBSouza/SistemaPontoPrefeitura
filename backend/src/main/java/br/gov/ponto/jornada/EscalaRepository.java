package br.gov.ponto.jornada;

import br.gov.ponto.jornada.domain.Escala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EscalaRepository extends JpaRepository<Escala, UUID> {

    List<Escala> findByVinculoIdAndTenantId(UUID vinculoId, UUID tenantId);

    List<Escala> findByTenantId(UUID tenantId);

    java.util.Optional<Escala> findByIdAndTenantId(UUID id, UUID tenantId);
}
