package br.gov.ponto.calendario;

import br.gov.ponto.calendario.domain.EventoCalendario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CalendarioRepository extends JpaRepository<EventoCalendario, UUID> {

    List<EventoCalendario> findByTenantIdAndData(UUID tenantId, LocalDate data);

    List<EventoCalendario> findByTenantIdAndDataBetweenOrderByData(UUID tenantId, LocalDate inicio, LocalDate fim);
}
