package br.gov.ponto.jornada;

import br.gov.ponto.jornada.domain.JornadaHorario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JornadaHorarioRepository extends JpaRepository<JornadaHorario, UUID> {

    List<JornadaHorario> findByJornadaIdAndTenantId(UUID jornadaId, UUID tenantId);

    void deleteByJornadaIdAndTenantId(UUID jornadaId, UUID tenantId);
}
