package br.gov.ponto.saas;

import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.relatorios.IndicadoresService;
import br.gov.ponto.relatorios.api.IndicadoresResponse;
import br.gov.ponto.saas.api.ContratoRequest;
import br.gov.ponto.saas.api.ContratoResponse;
import br.gov.ponto.saas.api.ExecucaoContratoResponse;
import br.gov.ponto.saas.domain.Contrato;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de fornecimento ao ente (valor FIXO — dispensa/licitação). Substitui o antigo
 * "billing por servidor ativo", que não cabe na contratação pública (por demanda). O relatório de
 * execução mensal reaproveita os indicadores do período para anexar ao processo de pagamento.
 */
@Service
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final IndicadoresService indicadoresService;

    public ContratoService(ContratoRepository contratoRepository, IndicadoresService indicadoresService) {
        this.contratoRepository = contratoRepository;
        this.indicadoresService = indicadoresService;
    }

    @Transactional
    public ContratoResponse criar(ContratoRequest req) {
        if (req.vigenciaFim().isBefore(req.vigenciaInicio())) {
            throw new IllegalArgumentException("A vigência final não pode ser anterior à inicial.");
        }
        Contrato c = contratoRepository.save(new Contrato(TenantContext.requireCurrent(),
                req.modalidade(), req.numeroProcesso(), req.empenho(), req.vigenciaInicio(),
                req.vigenciaFim(), req.valorGlobal(), req.valorMensal(), req.observacao()));
        return ContratoResponse.from(c, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<ContratoResponse> listar() {
        LocalDate hoje = LocalDate.now();
        return contratoRepository.findByTenantIdOrderByVigenciaInicioDesc(TenantContext.requireCurrent())
                .stream().map(c -> ContratoResponse.from(c, hoje)).toList();
    }

    @Transactional
    public void remover(UUID id) {
        TenantContext.requireCurrent();
        Contrato c = contratoRepository.findById(id) // RLS escopa ao tenant corrente
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contrato inexistente"));
        contratoRepository.delete(c);
    }

    /** Relatório de execução mensal (p/ anexar ao processo de pagamento/liquidação). */
    @Transactional(readOnly = true)
    public ExecucaoContratoResponse execucao(YearMonth competencia) {
        LocalDate ref = competencia.atEndOfMonth();
        Optional<Contrato> vigente = contratoRepository
                .findByTenantIdOrderByVigenciaInicioDesc(TenantContext.requireCurrent()).stream()
                .filter(c -> c.vigenteEm(ref))
                .findFirst();
        IndicadoresResponse ind = indicadoresService.obter(competencia);
        return new ExecucaoContratoResponse(
                competencia.toString(),
                vigente.isPresent(),
                vigente.map(Contrato::getValorMensal).orElse(null),
                ind.totalVinculos(),
                ind.dispositivosAtivos(),
                ind.registrosNoPeriodo(),
                vigente.map(Contrato::getNumeroProcesso).orElse(null),
                vigente.map(Contrato::getEmpenho).orElse(null));
    }
}
