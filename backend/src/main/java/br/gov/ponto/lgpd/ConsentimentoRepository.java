package br.gov.ponto.lgpd;

import br.gov.ponto.lgpd.domain.Consentimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, UUID> {

    Optional<Consentimento> findFirstByServidorIdAndTenantIdAndFinalidadeOrderByRegistradoEmDesc(
            UUID servidorId, UUID tenantId, String finalidade);
}
