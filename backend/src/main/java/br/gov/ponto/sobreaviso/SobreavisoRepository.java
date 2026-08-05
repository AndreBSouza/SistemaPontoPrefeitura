package br.gov.ponto.sobreaviso;

import br.gov.ponto.sobreaviso.domain.Sobreaviso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SobreavisoRepository extends JpaRepository<Sobreaviso, UUID> {

    List<Sobreaviso> findByVinculoIdAndTenantIdOrderByDataDesc(UUID vinculoId, UUID tenantId);

    List<Sobreaviso> findByVinculoIdAndTenantIdAndDataBetween(
            UUID vinculoId, UUID tenantId, LocalDate inicio, LocalDate fim);

    Optional<Sobreaviso> findByIdAndTenantId(UUID id, UUID tenantId);
}
