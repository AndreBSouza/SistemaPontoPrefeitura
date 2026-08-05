package br.gov.ponto.calendario;

import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.calendario.domain.EventoCalendario;
import br.gov.ponto.calendario.domain.TipoEventoCalendario;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Calendário oficial do município (12.4.5 / 12.1.6): feriados, pontos facultativos e
 * abonos coletivos. Um evento (geral ou por órgão) torna a data um dia não útil na
 * apuração — não gera falta e o trabalho do dia vira hora extra (regra no núcleo de cálculo).
 */
@Service
public class CalendarioService {

    private final CalendarioRepository calendarioRepository;
    private final VinculoRepository vinculoRepository;

    public CalendarioService(CalendarioRepository calendarioRepository, VinculoRepository vinculoRepository) {
        this.calendarioRepository = calendarioRepository;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional
    public EventoCalendario criar(LocalDate data, TipoEventoCalendario tipo, String descricao, UUID lotacaoId) {
        UUID tenantId = TenantContext.requireCurrent();
        return calendarioRepository.save(new EventoCalendario(tenantId, data, tipo, descricao, lotacaoId));
    }

    @Transactional
    public void remover(UUID id) {
        TenantContext.requireCurrent();
        // RLS escopa o findById ao tenant corrente: outro ente nem enxerga a linha.
        EventoCalendario evento = calendarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento inexistente"));
        calendarioRepository.delete(evento);
    }

    @Transactional(readOnly = true)
    public List<EventoCalendario> listar(YearMonth competencia) {
        return calendarioRepository.findByTenantIdAndDataBetweenOrderByData(
                TenantContext.requireCurrent(), competencia.atDay(1), competencia.atEndOfMonth());
    }

    /**
     * A data é dia não útil para o vínculo? Verdadeiro se houver evento geral na data
     * ou evento direcionado ao órgão em que o vínculo está lotado.
     */
    @Transactional(readOnly = true)
    public boolean diaNaoUtilParaVinculo(UUID vinculoId, LocalDate data) {
        UUID tenantId = TenantContext.requireCurrent();
        List<EventoCalendario> eventos = calendarioRepository.findByTenantIdAndData(tenantId, data);
        if (eventos.isEmpty()) {
            return false;
        }
        UUID lotacaoId = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .map(Vinculo::getLotacaoId).orElse(null);
        return eventos.stream()
                .anyMatch(e -> e.isGeral() || (lotacaoId != null && lotacaoId.equals(e.getLotacaoId())));
    }
}
