package br.gov.ponto.ausencia;

import br.gov.ponto.ausencia.domain.AusenciaProgramada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AusenciaRepository extends JpaRepository<AusenciaProgramada, UUID> {

    /** Ausência do vínculo que cobre a data (dataInicio <= data <= dataFim). */
    boolean existsByTenantIdAndVinculoIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
            UUID tenantId, UUID vinculoId, LocalDate data, LocalDate dataMesma);

    /** Ausências do ente que se sobrepõem ao intervalo [inicio, fim]. */
    List<AusenciaProgramada> findByTenantIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
            UUID tenantId, LocalDate fim, LocalDate inicio);

    /** Ausências dos vínculos informados que se sobrepõem ao intervalo [inicio, fim]. */
    List<AusenciaProgramada> findByTenantIdAndVinculoIdInAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
            UUID tenantId, Collection<UUID> vinculoIds, LocalDate fim, LocalDate inicio);

    /** Ausências de um vínculo, da mais recente para a mais antiga (autoatendimento no app). */
    List<AusenciaProgramada> findByTenantIdAndVinculoIdOrderByDataInicioDesc(UUID tenantId, UUID vinculoId);
}
