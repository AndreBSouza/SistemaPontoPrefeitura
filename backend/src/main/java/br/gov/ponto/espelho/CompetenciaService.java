package br.gov.ponto.espelho;

import br.gov.ponto.auditoria.AuditoriaService;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.espelho.domain.Competencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompetenciaService {

    private final CompetenciaRepository competenciaRepository;
    private final AuditoriaService auditoriaService;

    public CompetenciaService(CompetenciaRepository competenciaRepository,
                              AuditoriaService auditoriaService) {
        this.competenciaRepository = competenciaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public Competencia fechar(UUID vinculoId, YearMonth competencia) {
        Competencia c = obterOuCriar(vinculoId, competencia);
        if (c.isFechada()) {
            throw new ConflitoException("Competencia ja esta fechada");
        }
        c.fechar();
        Competencia salva = competenciaRepository.save(c);
        auditoriaService.registrar("FECHAMENTO", "competencia", salva.getId().toString(),
                "Competencia " + competencia + " fechada");
        return salva;
    }

    /**
     * Fechamento em lote (12.6.2): fecha a competência dos vínculos informados, pulando
     * os que já estão fechados (idempotente). Cada fechamento é auditado individualmente.
     */
    @Transactional
    public List<Competencia> fecharEmLote(List<UUID> vinculoIds, YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        LocalDate anoMes = competencia.atDay(1);
        List<Competencia> fechadas = new ArrayList<>();
        for (UUID vinculoId : vinculoIds) {
            boolean jaFechada = competenciaRepository
                    .findByVinculoIdAndTenantIdAndAnoMes(vinculoId, tenantId, anoMes)
                    .map(Competencia::isFechada).orElse(false);
            if (jaFechada) {
                continue;
            }
            fechadas.add(fechar(vinculoId, competencia));
        }
        return fechadas;
    }

    @Transactional
    public Competencia reabrir(UUID vinculoId, YearMonth competencia, String motivo) {
        UUID tenantId = TenantContext.requireCurrent();
        Competencia c = competenciaRepository
                .findByVinculoIdAndTenantIdAndAnoMes(vinculoId, tenantId, competencia.atDay(1))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Competencia inexistente"));
        if (!c.isFechada()) {
            throw new ConflitoException("Competencia nao esta fechada");
        }
        c.reabrir(motivo);
        Competencia salva = competenciaRepository.save(c);
        auditoriaService.registrar("REABERTURA", "competencia", salva.getId().toString(),
                "Competencia " + competencia + " reaberta: " + motivo);
        return salva;
    }

    @Transactional
    public Competencia darCiencia(UUID vinculoId, YearMonth competencia, String evidencia) {
        Competencia c = obterOuCriar(vinculoId, competencia);
        c.darCiencia(evidencia);
        return competenciaRepository.save(c);
    }

    @Transactional(readOnly = true)
    public Optional<Competencia> buscar(UUID vinculoId, YearMonth competencia) {
        return competenciaRepository.findByVinculoIdAndTenantIdAndAnoMes(
                vinculoId, TenantContext.requireCurrent(), competencia.atDay(1));
    }

    @Transactional(readOnly = true)
    public boolean estaFechada(UUID vinculoId, LocalDate data) {
        return buscar(vinculoId, YearMonth.from(data)).map(Competencia::isFechada).orElse(false);
    }

    private Competencia obterOuCriar(UUID vinculoId, YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        return competenciaRepository
                .findByVinculoIdAndTenantIdAndAnoMes(vinculoId, tenantId, competencia.atDay(1))
                .orElseGet(() -> competenciaRepository.save(
                        new Competencia(tenantId, vinculoId, competencia.atDay(1))));
    }
}
