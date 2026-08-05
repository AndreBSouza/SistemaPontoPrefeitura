package br.gov.ponto.ausencia;

import br.gov.ponto.ausencia.api.CoberturaResponse;
import br.gov.ponto.ausencia.domain.AusenciaProgramada;
import br.gov.ponto.ausencia.domain.TipoAusencia;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gestão de férias e licenças (12.4.1): programação de ausências, visão de cobertura da
 * equipe por órgão (impede deixar o setor descoberto) e neutralização da falta na apuração.
 */
@Service
public class AusenciaService {

    private final AusenciaRepository ausenciaRepository;
    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;

    public AusenciaService(AusenciaRepository ausenciaRepository, VinculoRepository vinculoRepository,
                           ServidorRepository servidorRepository) {
        this.ausenciaRepository = ausenciaRepository;
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
    }

    @Transactional
    public AusenciaProgramada agendar(UUID vinculoId, TipoAusencia tipo, LocalDate dataInicio,
                                      LocalDate dataFim, String observacao) {
        UUID tenantId = TenantContext.requireCurrent();
        if (!vinculoRepository.existsByIdAndTenantId(vinculoId, tenantId)) {
            throw new IllegalArgumentException("Vinculo inexistente no ente");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("dataFim nao pode ser anterior a dataInicio");
        }
        return ausenciaRepository.save(
                new AusenciaProgramada(tenantId, vinculoId, tipo, dataInicio, dataFim, observacao));
    }

    @Transactional
    public void remover(UUID id) {
        TenantContext.requireCurrent();
        // RLS escopa o findById ao tenant corrente.
        AusenciaProgramada a = ausenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ausência inexistente"));
        ausenciaRepository.delete(a);
    }

    /** Ausências do ente que tocam a competência. */
    @Transactional(readOnly = true)
    public List<AusenciaProgramada> listar(YearMonth competencia) {
        return ausenciaRepository.findByTenantIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
                TenantContext.requireCurrent(), competencia.atEndOfMonth(), competencia.atDay(1));
    }

    /** Ausências de um vínculo (autoatendimento do servidor no app). */
    @Transactional(readOnly = true)
    public List<AusenciaProgramada> porVinculo(UUID vinculoId) {
        return ausenciaRepository.findByTenantIdAndVinculoIdOrderByDataInicioDesc(
                TenantContext.requireCurrent(), vinculoId);
    }

    /** A data está dentro de uma ausência programada do vínculo? (usado pela apuração). */
    @Transactional(readOnly = true)
    public boolean estaAusente(UUID vinculoId, LocalDate data) {
        return ausenciaRepository
                .existsByTenantIdAndVinculoIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
                        TenantContext.requireCurrent(), vinculoId, data, data);
    }

    /** Cobertura da equipe de um órgão na competência: quem está ausente e quando. */
    @Transactional(readOnly = true)
    public CoberturaResponse cobertura(UUID lotacaoId, YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        List<Vinculo> vinculos = vinculoRepository.findByLotacaoIdInAndTenantId(List.of(lotacaoId), tenantId);
        List<UUID> vinculoIds = vinculos.stream().map(Vinculo::getId).toList();
        Map<UUID, String> nomes = servidorRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Servidor::getId, Servidor::getNome, (a, b) -> a));
        Map<UUID, UUID> servidorDoVinculo = vinculos.stream()
                .collect(Collectors.toMap(Vinculo::getId, Vinculo::getServidorId, (a, b) -> a));

        List<CoberturaResponse.Item> itens = vinculoIds.isEmpty() ? List.of()
                : ausenciaRepository
                .findByTenantIdAndVinculoIdInAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
                        tenantId, vinculoIds, competencia.atEndOfMonth(), competencia.atDay(1)).stream()
                .map(a -> new CoberturaResponse.Item(a.getVinculoId(),
                        nomes.getOrDefault(servidorDoVinculo.get(a.getVinculoId()), "?"),
                        a.getTipo(), a.getDataInicio(), a.getDataFim()))
                .toList();

        return new CoberturaResponse(lotacaoId, competencia.toString(), vinculos.size(), itens);
    }
}
