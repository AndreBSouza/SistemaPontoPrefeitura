package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.domain.GeofenceLocal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeofenceLocalRepository extends JpaRepository<GeofenceLocal, UUID> {

    List<GeofenceLocal> findByTenantIdAndLotacaoIdOrderByNome(UUID tenantId, UUID lotacaoId);

    Optional<GeofenceLocal> findByIdAndTenantId(UUID id, UUID tenantId);
}
