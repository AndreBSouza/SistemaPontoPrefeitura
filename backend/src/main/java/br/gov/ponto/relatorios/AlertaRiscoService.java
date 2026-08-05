package br.gov.ponto.relatorios;

import br.gov.ponto.bancohoras.BancoHorasRepository;
import br.gov.ponto.bancohoras.domain.TipoLancamento;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.relatorios.api.AlertasResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Alertas de risco proativos (12.6.10): ajustes manuais de banco de horas e batidas
 * fora da cerca no período — insumos baratos e de alto sinal para o controle interno.
 */
@Service
public class AlertaRiscoService {

    private final BancoHorasRepository bancoHorasRepository;
    private final RegistroPontoRepository registroRepository;
    private final ServidorRepository servidorRepository;
    private final VinculoRepository vinculoRepository;

    public AlertaRiscoService(BancoHorasRepository bancoHorasRepository,
                              RegistroPontoRepository registroRepository,
                              ServidorRepository servidorRepository,
                              VinculoRepository vinculoRepository) {
        this.bancoHorasRepository = bancoHorasRepository;
        this.registroRepository = registroRepository;
        this.servidorRepository = servidorRepository;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional(readOnly = true)
    public AlertasResponse alertas(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        LocalDate inicio = competencia.atDay(1);
        LocalDate fim = competencia.atEndOfMonth();
        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);

        Map<UUID, String> nomes = servidorRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Servidor::getId, Servidor::getNome, (a, b) -> a));
        Map<UUID, UUID> servidorDoVinculo = vinculoRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Vinculo::getId, Vinculo::getServidorId, (a, b) -> a));
        Function<UUID, String> servidor = vid ->
                nomes.getOrDefault(servidorDoVinculo.get(vid), "?");

        List<AlertasResponse.AjusteManual> ajustes = bancoHorasRepository
                .findByTenantIdAndTipoAndDataBetween(tenantId, TipoLancamento.AJUSTE, inicio, fim).stream()
                .map(l -> new AlertasResponse.AjusteManual(l.getVinculoId(), servidor.apply(l.getVinculoId()),
                        l.getData(), l.getMinutos(), l.getDescricao()))
                .toList();

        Map<UUID, Long> foraPorVinculo = registroRepository
                .findByTenantIdAndDataHoraServidorBetweenOrderByNsr(tenantId, periodo[0], periodo[1]).stream()
                .filter(RegistroPonto::isForaDaCerca)
                .collect(Collectors.groupingBy(RegistroPonto::getVinculoId, Collectors.counting()));
        List<AlertasResponse.ForaDaCerca> fora = foraPorVinculo.entrySet().stream()
                .map(e -> new AlertasResponse.ForaDaCerca(e.getKey(), servidor.apply(e.getKey()), e.getValue()))
                .toList();

        return new AlertasResponse(ajustes, fora);
    }
}
