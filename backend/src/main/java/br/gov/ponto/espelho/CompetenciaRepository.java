package br.gov.ponto.espelho;

import br.gov.ponto.espelho.domain.Competencia;
import br.gov.ponto.espelho.domain.StatusCompetencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompetenciaRepository extends JpaRepository<Competencia, UUID> {

    Optional<Competencia> findByVinculoIdAndTenantIdAndAnoMes(UUID vinculoId, UUID tenantId, LocalDate anoMes);

    /** Competências fechadas que ainda aguardam ciência do servidor (lembretes de pendência). */
    List<Competencia> findByTenantIdAndStatusAndCienciaEmIsNull(UUID tenantId, StatusCompetencia status);
}
