package br.gov.ponto.projeto;

import br.gov.ponto.projeto.domain.ApropriacaoHoras;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ApropriacaoRepository extends JpaRepository<ApropriacaoHoras, UUID> {

    List<ApropriacaoHoras> findByTenantIdAndDataBetween(UUID tenantId, LocalDate inicio, LocalDate fim);
}
