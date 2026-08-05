package br.gov.ponto.correcao;

import br.gov.ponto.correcao.domain.CorrecaoMarcacao;
import br.gov.ponto.correcao.domain.StatusCorrecao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorrecaoRepository extends JpaRepository<CorrecaoMarcacao, UUID> {

    Optional<CorrecaoMarcacao> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CorrecaoMarcacao> findByTenantIdAndStatusOrderBySolicitadoEmDesc(UUID tenantId, StatusCorrecao status);

    List<CorrecaoMarcacao> findByVinculoIdAndTenantIdOrderBySolicitadoEmDesc(UUID vinculoId, UUID tenantId);
}
