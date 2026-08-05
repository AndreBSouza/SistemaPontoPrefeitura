package br.gov.ponto.registro;

import br.gov.ponto.registro.domain.EventoRep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventoRepRepository extends JpaRepository<EventoRep, UUID> {

    /** Eventos do período, na ordem do NSR (o AFD exige os registros ordenados por NSR). */
    List<EventoRep> findByTenantIdAndDataHoraBetweenOrderByNsr(UUID tenantId, Instant inicio, Instant fim);
}
