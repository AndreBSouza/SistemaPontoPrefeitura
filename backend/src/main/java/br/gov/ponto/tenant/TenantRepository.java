package br.gov.ponto.tenant;

import br.gov.ponto.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    boolean existsBySlug(String slug);

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findBySubdominio(String subdominio);

    boolean existsBySubdominio(String subdominio);
}
