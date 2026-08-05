package br.gov.ponto.saas;

import br.gov.ponto.saas.domain.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContratoRepository extends JpaRepository<Contrato, UUID> {

    List<Contrato> findByTenantIdOrderByVigenciaInicioDesc(UUID tenantId);
}
