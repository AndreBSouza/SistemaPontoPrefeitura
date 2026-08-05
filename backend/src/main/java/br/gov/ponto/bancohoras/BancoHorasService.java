package br.gov.ponto.bancohoras;

import br.gov.ponto.apuracao.ApuracaoService;
import br.gov.ponto.apuracao.domain.ApuracaoDia;
import br.gov.ponto.apuracao.domain.Ocorrencia;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.bancohoras.domain.BancoDeHoras;
import br.gov.ponto.bancohoras.domain.BancoHorasLancamento;
import br.gov.ponto.bancohoras.domain.TipoLancamento;
import br.gov.ponto.cadastro.RegrasPontoService;
import br.gov.ponto.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Banco de horas: credita horas extras e debita atrasos/saidas antecipadas a partir
 * da apuracao do dia, respeitando um limite de acumulo; suporta compensacao,
 * ajuste manual e prescricao de creditos antigos.
 */
@Service
public class BancoHorasService {

    /** Teto padrao do sistema para acumulo do saldo (200 horas); o orgao pode reduzir/ampliar. */
    private static final int LIMITE_SALDO_MIN = 200 * 60;

    private final BancoHorasRepository repository;
    private final ApuracaoService apuracaoService;
    private final RegrasPontoService regrasPontoService;

    public BancoHorasService(BancoHorasRepository repository, ApuracaoService apuracaoService,
                             RegrasPontoService regrasPontoService) {
        this.repository = repository;
        this.apuracaoService = apuracaoService;
        this.regrasPontoService = regrasPontoService;
    }

    @Transactional(readOnly = true)
    public int saldo(UUID vinculoId) {
        return repository.saldo(vinculoId, TenantContext.requireCurrent());
    }

    @Transactional
    public int lancarApuracaoDoDia(UUID vinculoId, LocalDate data) {
        UUID tenantId = TenantContext.requireCurrent();
        // Respeita a politica do orgao: se banco de horas desabilitado, nao lanca.
        var regras = regrasPontoService.regrasDoOrgaoDoVinculo(vinculoId);
        if (!regras.bancoHorasHabilitado()) {
            return repository.saldo(vinculoId, tenantId);
        }
        ApuracaoDia ap = apuracaoService.apurarDia(vinculoId, data);
        int credito = soma(ap, TipoOcorrencia.HORA_EXTRA);
        int debito = soma(ap, TipoOcorrencia.ATRASO) + soma(ap, TipoOcorrencia.SAIDA_ANTECIPADA);
        int net = credito - debito;
        int limite = regras.tetoBancoHorasMinutos(LIMITE_SALDO_MIN);
        int aplicar = BancoDeHoras.de(repository.saldo(vinculoId, tenantId), limite)
                .aplicarLiquido(net);
        if (aplicar != 0) {
            repository.save(new BancoHorasLancamento(tenantId, vinculoId, data, aplicar,
                    TipoLancamento.APURACAO, "Apuracao do dia " + data));
        }
        return repository.saldo(vinculoId, tenantId);
    }

    @Transactional
    public int compensar(UUID vinculoId, int minutos, LocalDate data, String descricao) {
        UUID tenantId = TenantContext.requireCurrent();
        int limite = regrasPontoService.regrasDoOrgaoDoVinculo(vinculoId).tetoBancoHorasMinutos(LIMITE_SALDO_MIN);
        BancoDeHoras.de(repository.saldo(vinculoId, tenantId), limite)
                .validarCompensacao(minutos);
        repository.save(new BancoHorasLancamento(tenantId, vinculoId, data, -minutos,
                TipoLancamento.COMPENSACAO, descricao));
        return repository.saldo(vinculoId, tenantId);
    }

    @Transactional
    public int ajustar(UUID vinculoId, LocalDate data, int minutos, String descricao) {
        UUID tenantId = TenantContext.requireCurrent();
        repository.save(new BancoHorasLancamento(tenantId, vinculoId, data, minutos,
                TipoLancamento.AJUSTE, descricao));
        return repository.saldo(vinculoId, tenantId);
    }

    @Transactional
    public int prescrever(UUID vinculoId, LocalDate dataLimite) {
        UUID tenantId = TenantContext.requireCurrent();
        int creditosAntigos = repository.creditosAntesDe(vinculoId, tenantId, dataLimite);
        if (creditosAntigos > 0) {
            repository.save(new BancoHorasLancamento(tenantId, vinculoId, dataLimite, -creditosAntigos,
                    TipoLancamento.PRESCRICAO, "Prescricao de creditos anteriores a " + dataLimite));
        }
        return repository.saldo(vinculoId, tenantId);
    }

    private int soma(ApuracaoDia apuracao, TipoOcorrencia tipo) {
        return apuracao.ocorrencias().stream()
                .filter(o -> o.tipo() == tipo)
                .mapToInt(Ocorrencia::minutos)
                .sum();
    }
}
