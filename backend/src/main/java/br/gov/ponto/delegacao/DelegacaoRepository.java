package br.gov.ponto.delegacao;

import br.gov.ponto.delegacao.domain.Delegacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DelegacaoRepository extends JpaRepository<Delegacao, UUID> {

    Optional<Delegacao> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Delegacao> findByTenantIdOrderByCriadoEmDesc(UUID tenantId);

    /** Delegações ativas para o substituto, vigentes na data. */
    List<Delegacao> findByTenantIdAndDelegadoServidorIdAndAtivoTrueAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
            UUID tenantId, UUID delegadoServidorId, LocalDate data, LocalDate dataMesma);
}
