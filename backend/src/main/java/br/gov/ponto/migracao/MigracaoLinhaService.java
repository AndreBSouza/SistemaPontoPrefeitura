package br.gov.ponto.migracao;

import br.gov.ponto.bancohoras.BancoHorasRepository;
import br.gov.ponto.bancohoras.domain.BancoHorasLancamento;
import br.gov.ponto.bancohoras.domain.TipoLancamento;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Vinculo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Importa UMA linha de saldo em sua PRÓPRIA transação ({@link Propagation#REQUIRES_NEW}): assim uma
 * falha de persistência numa linha não contamina (rollback-only) nem reverte as demais do lote, e o
 * erro é atribuído à linha certa. A GUC de tenant (RLS) é reaplicada na nova conexão pelo
 * {@code TenantAwareDataSource}, então o isolamento por ente se mantém.
 */
@Service
public class MigracaoLinhaService {

    private final VinculoRepository vinculoRepository;
    private final BancoHorasRepository bancoHorasRepository;

    public MigracaoLinhaService(VinculoRepository vinculoRepository, BancoHorasRepository bancoHorasRepository) {
        this.vinculoRepository = vinculoRepository;
        this.bancoHorasRepository = bancoHorasRepository;
    }

    /**
     * @return {@code true} se importou; {@code false} se já havia MIGRACAO (idempotente).
     *         Lança {@code IllegalArgumentException} (matrícula inexistente) ou
     *         {@code DataIntegrityViolationException} (corrida no índice único) para o chamador tratar.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean importar(UUID tenantId, String matricula, int minutos, LocalDate dataReferencia) {
        Vinculo vinculo = vinculoRepository.findByTenantIdAndMatricula(tenantId, matricula)
                .orElseThrow(() -> new IllegalArgumentException("Matrícula inexistente: " + matricula));
        if (bancoHorasRepository.existsByVinculoIdAndTenantIdAndTipo(
                vinculo.getId(), tenantId, TipoLancamento.MIGRACAO)) {
            return false;
        }
        // saveAndFlush: um erro de banco (ex.: corrida no índice único) aflora AQUI e reverte só esta
        // transação/linha — nunca no commit do lote.
        bancoHorasRepository.saveAndFlush(new BancoHorasLancamento(tenantId, vinculo.getId(), dataReferencia,
                minutos, TipoLancamento.MIGRACAO, "Saldo inicial (migração)"));
        return true;
    }
}
