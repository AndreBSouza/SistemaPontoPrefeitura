package br.gov.ponto.registro;

import br.gov.ponto.registro.domain.RegistroPonto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistroPontoRepository extends JpaRepository<RegistroPonto, UUID> {

    Optional<RegistroPonto> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<RegistroPonto> findByVinculoIdAndTenantIdOrderByNsr(UUID vinculoId, UUID tenantId);

    List<RegistroPonto> findByVinculoIdAndTenantIdAndDataHoraServidorBetweenOrderByDataHoraServidor(
            UUID vinculoId, UUID tenantId, Instant inicio, Instant fim);

    List<RegistroPonto> findByTenantIdAndDataHoraServidorBetweenOrderByNsr(
            UUID tenantId, Instant inicio, Instant fim);

    long countByTenantIdAndDataHoraServidorBetween(UUID tenantId, Instant inicio, Instant fim);

    /** Último registro do tenant (maior NSR) — usado para encadear o hash de integridade. */
    Optional<RegistroPonto> findTopByTenantIdOrderByNsrDesc(UUID tenantId);
}
