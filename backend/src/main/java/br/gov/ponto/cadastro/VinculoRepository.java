package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.domain.Vinculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VinculoRepository extends JpaRepository<Vinculo, UUID> {

    List<Vinculo> findByServidorIdAndTenantId(UUID servidorId, UUID tenantId);

    List<Vinculo> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndMatricula(UUID tenantId, String matricula);

    Optional<Vinculo> findByTenantIdAndMatricula(UUID tenantId, String matricula);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Vinculo> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Vinculo> findByLotacaoIdInAndTenantId(Collection<UUID> lotacaoIds, UUID tenantId);
}
