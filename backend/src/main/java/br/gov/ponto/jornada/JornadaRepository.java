package br.gov.ponto.jornada;

import br.gov.ponto.jornada.domain.Jornada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JornadaRepository extends JpaRepository<Jornada, UUID> {

    List<Jornada> findByTenantId(UUID tenantId);

    Optional<Jornada> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNome(UUID tenantId, String nome);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
