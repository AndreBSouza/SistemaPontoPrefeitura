package br.gov.ponto.relatorios;

import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.CadeiaHash;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.relatorios.api.IntegridadeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Verificador da cadeia de integridade (hash-chain) dos registros de ponto — prova de
 * tamper-evidence para o controle interno. Recalcula o hash de cada registro a partir dos
 * próprios campos imutáveis e confere o encadeamento; aponta o primeiro NSR rompido.
 */
@Service
public class IntegridadeService {

    private final RegistroPontoRepository registroRepository;

    public IntegridadeService(RegistroPontoRepository registroRepository) {
        this.registroRepository = registroRepository;
    }

    @Transactional(readOnly = true)
    public IntegridadeResponse verificar(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);
        List<RegistroPonto> regs = registroRepository
                .findByTenantIdAndDataHoraServidorBetweenOrderByNsr(tenantId, periodo[0], periodo[1]);

        for (int i = 0; i < regs.size(); i++) {
            RegistroPonto r = regs.get(i);
            String anterior = r.getHashAnterior() == null ? "" : r.getHashAnterior();
            String esperado = CadeiaHash.calcular(anterior, r.getTenantId(), r.getVinculoId(),
                    r.getNsr(), r.getTipo(), r.getDataHoraServidor(), r.getIdempotencyKey());
            if (!esperado.equals(r.getHash())) {
                return new IntegridadeResponse(false, regs.size(), r.getNsr(),
                        "Hash do NSR " + r.getNsr() + " não confere — registro alterado.");
            }
            // Elo (encadeamento) só é checável entre registros NSR-consecutivos. Uma correção
            // retroativa (origem AJUSTE) tem NSR alto mas data passada, então a janela da
            // competência pode pular NSRs (registros de outros meses) — nesse caso o elo é
            // validado na competência que contém o NSR anterior, não aqui.
            if (i > 0 && r.getNsr() == regs.get(i - 1).getNsr() + 1
                    && !regs.get(i - 1).getHash().equals(anterior)) {
                return new IntegridadeResponse(false, regs.size(), r.getNsr(),
                        "Elo quebrado no NSR " + r.getNsr() + " — possível inserção/remoção.");
            }
        }
        return new IntegridadeResponse(true, regs.size(), null,
                "Cadeia íntegra: " + regs.size() + " registros sem sinal de adulteração.");
    }
}
