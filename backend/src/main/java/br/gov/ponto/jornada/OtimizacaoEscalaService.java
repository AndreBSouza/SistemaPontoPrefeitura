package br.gov.ponto.jornada;

import br.gov.ponto.ausencia.AusenciaService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.api.SugestaoEscalaResponse;
import br.gov.ponto.jornada.domain.OtimizadorEscala;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Sugestão de escala (12.4.11): monta uma distribuição justa da cobertura diária de um órgão,
 * respeitando as ausências programadas (férias/licenças) e um teto de dias consecutivos. Apenas
 * sugere — não grava escala.
 */
@Service
public class OtimizacaoEscalaService {

    private final VinculoRepository vinculoRepository;
    private final AusenciaService ausenciaService;

    public OtimizacaoEscalaService(VinculoRepository vinculoRepository, AusenciaService ausenciaService) {
        this.vinculoRepository = vinculoRepository;
        this.ausenciaService = ausenciaService;
    }

    @Transactional(readOnly = true)
    public SugestaoEscalaResponse sugerir(UUID lotacaoId, YearMonth competencia,
                                          int coberturaPorDia, int maxConsecutivos) {
        if (coberturaPorDia < 1) {
            throw new IllegalArgumentException("coberturaPorDia deve ser >= 1");
        }
        if (maxConsecutivos < 1) {
            throw new IllegalArgumentException("maxConsecutivos deve ser >= 1");
        }
        UUID tenantId = TenantContext.requireCurrent();
        List<UUID> servidores = vinculoRepository.findByLotacaoIdInAndTenantId(List.of(lotacaoId), tenantId)
                .stream().filter(Vinculo::isAtivo).map(Vinculo::getId).toList();

        List<LocalDate> dias = new ArrayList<>();
        for (int d = 1; d <= competencia.lengthOfMonth(); d++) {
            dias.add(competencia.atDay(d));
        }
        BiPredicate<UUID, LocalDate> disponivel = (v, data) -> !ausenciaService.estaAusente(v, data);

        OtimizadorEscala.Sugestao sugestao =
                OtimizadorEscala.montar(servidores, dias, disponivel, coberturaPorDia, maxConsecutivos);

        List<SugestaoEscalaResponse.DiaSugestao> diasResp = new ArrayList<>();
        for (LocalDate dia : dias) {
            diasResp.add(new SugestaoEscalaResponse.DiaSugestao(
                    dia, sugestao.escala().getOrDefault(dia, List.of())));
        }
        List<String> descobertos = sugestao.diasDescobertos().stream().map(LocalDate::toString).toList();

        return new SugestaoEscalaResponse(competencia.toString(), coberturaPorDia, maxConsecutivos,
                servidores.size(), diasResp, descobertos);
    }
}
