package br.gov.ponto.tenant;

import br.gov.ponto.tenant.domain.TenantLogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantLogoRepository extends JpaRepository<TenantLogo, UUID> {
}
