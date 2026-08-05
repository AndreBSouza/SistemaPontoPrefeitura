package br.gov.ponto.registro;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.domain.EventoRep;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Grava no ARP as operações do REP-P que não são marcação de ponto (Anexo IX, item 6) — elas
 * aparecem no AFD como registros dos tipos "5" (empregado) e "6" (evento sensível).
 *
 * <p>O NSR sai do {@link NsrGenerator}, a MESMA sequência das marcações: o Anexo IX exige uma
 * numeração única e sequencial por estabelecimento, contada desde a primeira operação do REP.
 * Se empregados e marcações tivessem sequências separadas, o AFD sairia com NSR duplicado.</p>
 */
@Service
public class EventoRepService {

    private final EventoRepRepository repository;
    private final NsrGenerator nsrGenerator;

    public EventoRepService(EventoRepRepository repository, NsrGenerator nsrGenerator) {
        this.repository = repository;
        this.nsrGenerator = nsrGenerator;
    }

    /** Inclusão de empregado no REP (operação "I"). */
    @Transactional
    public void empregadoIncluido(String cpf, String nome, String cpfResponsavel) {
        registrarEmpregado("I", cpf, nome, cpfResponsavel);
    }

    /** Alteração de dados do empregado no REP (operação "A"). */
    @Transactional
    public void empregadoAlterado(String cpf, String nome, String cpfResponsavel) {
        registrarEmpregado("A", cpf, nome, cpfResponsavel);
    }

    /** Exclusão do empregado no REP (operação "E"). */
    @Transactional
    public void empregadoExcluido(String cpf, String nome, String cpfResponsavel) {
        registrarEmpregado("E", cpf, nome, cpfResponsavel);
    }

    /** Evento sensível do REP-P: "02" retorno de energia, "07"/"08" (in)disponibilidade de serviço. */
    @Transactional
    public void eventoSensivel(String codigoEvento) {
        UUID tenantId = TenantContext.requireCurrent();
        repository.save(EventoRep.eventoSensivel(
                tenantId, nsrGenerator.next(tenantId), Instant.now(), codigoEvento));
    }

    private void registrarEmpregado(String operacao, String cpf, String nome, String cpfResponsavel) {
        UUID tenantId = TenantContext.requireCurrent();
        repository.save(EventoRep.empregado(
                tenantId, nsrGenerator.next(tenantId), Instant.now(), operacao, cpf, nome, cpfResponsavel));
    }
}
