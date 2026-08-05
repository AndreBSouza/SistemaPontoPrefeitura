package br.gov.ponto.bancohoras;

import br.gov.ponto.bancohoras.domain.BancoHorasLancamento;
import br.gov.ponto.bancohoras.domain.TipoLancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BancoHorasRepository extends JpaRepository<BancoHorasLancamento, UUID> {

    @Query("select coalesce(sum(l.minutos), 0) from BancoHorasLancamento l "
            + "where l.vinculoId = ?1 and l.tenantId = ?2")
    int saldo(UUID vinculoId, UUID tenantId);

    List<BancoHorasLancamento> findByTenantIdAndTipoAndDataBetween(
            UUID tenantId, TipoLancamento tipo, LocalDate inicio, LocalDate fim);

    @Query("select coalesce(sum(l.minutos), 0) from BancoHorasLancamento l "
            + "where l.vinculoId = ?1 and l.tenantId = ?2 and l.minutos > 0 and l.data < ?3")
    int creditosAntesDe(UUID vinculoId, UUID tenantId, LocalDate dataLimite);

    List<BancoHorasLancamento> findByVinculoIdAndTenantIdOrderByData(UUID vinculoId, UUID tenantId);

    boolean existsByVinculoIdAndTenantIdAndTipo(UUID vinculoId, UUID tenantId, TipoLancamento tipo);
}
