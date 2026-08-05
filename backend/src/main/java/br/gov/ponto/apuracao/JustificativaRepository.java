package br.gov.ponto.apuracao;

import br.gov.ponto.apuracao.domain.Justificativa;
import br.gov.ponto.apuracao.domain.StatusJustificativa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JustificativaRepository extends JpaRepository<Justificativa, UUID> {

    List<Justificativa> findByVinculoIdAndTenantId(UUID vinculoId, UUID tenantId);

    List<Justificativa> findByTenantIdAndStatus(UUID tenantId, StatusJustificativa status);

    List<Justificativa> findByVinculoIdInAndTenantIdAndStatus(
            Collection<UUID> vinculoIds, UUID tenantId, StatusJustificativa status);

    Optional<Justificativa> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Justificativa> findByVinculoIdAndTenantIdAndStatusAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
            UUID vinculoId, UUID tenantId, StatusJustificativa status, LocalDate ate, LocalDate desde);

    /** Justificativas (abonos) do ente que tocam o intervalo [desde, ate] — relatório de abonos. */
    List<Justificativa> findByTenantIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioDesc(
            UUID tenantId, LocalDate ate, LocalDate desde);
}
