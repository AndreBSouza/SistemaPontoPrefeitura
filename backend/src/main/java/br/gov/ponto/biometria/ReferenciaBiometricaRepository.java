package br.gov.ponto.biometria;

import br.gov.ponto.biometria.domain.ReferenciaBiometrica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReferenciaBiometricaRepository extends JpaRepository<ReferenciaBiometrica, UUID> {

    Optional<ReferenciaBiometrica> findByServidorIdAndTenantId(UUID servidorId, UUID tenantId);
}
