package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.RegrasEfetivas;
import br.gov.ponto.cadastro.domain.RegrasPonto;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.EscalaRepository;
import br.gov.ponto.jornada.JornadaRepository;
import br.gov.ponto.jornada.domain.Escala;
import br.gov.ponto.jornada.domain.Jornada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolve as regras de ponto efetivas de um vinculo aplicando a heranca do Orgao:
 * a jornada do vinculo (escala vigente) tem precedencia; na ausencia dela usa-se a
 * jornada padrao do orgao; tolerancia/banco de horas/geofence vem do orgao, com
 * defaults do sistema quando nao definidos.
 */
@Service
public class RegrasPontoService {

    private final VinculoRepository vinculoRepository;
    private final LotacaoRepository lotacaoRepository;
    private final EscalaRepository escalaRepository;
    private final JornadaRepository jornadaRepository;

    public RegrasPontoService(VinculoRepository vinculoRepository,
                              LotacaoRepository lotacaoRepository,
                              EscalaRepository escalaRepository,
                              JornadaRepository jornadaRepository) {
        this.vinculoRepository = vinculoRepository;
        this.lotacaoRepository = lotacaoRepository;
        this.escalaRepository = escalaRepository;
        this.jornadaRepository = jornadaRepository;
    }

    /** Regras (planas) do orgao em que o vinculo esta lotado; vazia() se sem lotacao/regras. */
    @Transactional(readOnly = true)
    public RegrasPonto regrasDoOrgaoDoVinculo(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        Vinculo vinculo = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vinculo inexistente"));
        return Optional.ofNullable(vinculo.getLotacaoId())
                .flatMap(id -> lotacaoRepository.findByIdAndTenantId(id, tenantId))
                .map(Lotacao::getRegras)
                .orElseGet(RegrasPonto::vazia);
    }

    @Transactional(readOnly = true)
    public RegrasEfetivas efetivasParaVinculo(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        RegrasPonto orgao = regrasDoOrgaoDoVinculo(vinculoId);

        // Override por vinculo: jornada da escala vigente. Fallback: jornada padrao do orgao.
        Optional<UUID> jornadaId = escalaRepository.findByVinculoIdAndTenantId(vinculoId, tenantId).stream()
                .filter(e -> e.vigenteEm(LocalDate.now(TempoMunicipal.ZONE)))
                .findFirst()
                .map(Escala::getJornadaId)
                .or(() -> Optional.ofNullable(orgao.getJornadaPadraoId()));
        Optional<Jornada> jornada = jornadaId.flatMap(id -> jornadaRepository.findByIdAndTenantId(id, tenantId));

        boolean comIntervalo = jornada.map(j -> j.getIntervaloMin() > 0).orElse(true);
        Integer tolerancia = orgao.getToleranciaMinutos() != null
                ? orgao.getToleranciaMinutos()
                : jornada.map(Jornada::getToleranciaMin).orElse(null);

        return new RegrasEfetivas(jornadaId.orElse(null), comIntervalo, tolerancia, orgao.bancoHorasHabilitado(),
                orgao.getGeofenceLatitude(), orgao.getGeofenceLongitude(), orgao.getGeofenceRaioMetros(),
                orgao.teletrabalho());
    }
}
