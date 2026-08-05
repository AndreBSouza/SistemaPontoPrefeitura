package br.gov.ponto.migracao;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.migracao.api.MigracaoSaldoResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Migração "zero-dor" (12.5.3, parte codável): importa o SALDO INICIAL de banco de horas de um
 * sistema anterior, para não perder o comp-time acumulado ao trocar de sistema. CSV
 * {@code matricula;saldoMinutos} (saldo pode ser negativo). <b>Idempotente</b> (índice único
 * {@code uq_banco_horas_migracao} + verificação): re-rodar o arquivo não dobra o saldo.
 *
 * <p>Cada linha é importada em sua PRÓPRIA transação ({@link MigracaoLinhaService}), então a falha
 * de uma linha não reverte o lote. A leitura do relógio físico (REP-C) fica de fora — o cliente não
 * trabalha com ponto físico inicialmente.</p>
 */
@Service
public class MigracaoService {

    /** Caractere BOM (U+FEFF) que o Excel prefixa em CSV UTF-8. */
    private static final String BOM = "﻿";

    private final MigracaoLinhaService linhaService;

    public MigracaoService(MigracaoLinhaService linhaService) {
        this.linhaService = linhaService;
    }

    // Sem @Transactional aqui de propósito: cada linha commita/reverte de forma independente.
    public MigracaoSaldoResponse importarSaldoBancoHoras(String csv, LocalDate dataReferencia) {
        UUID tenantId = TenantContext.requireCurrent();
        int importados = 0;
        int ignorados = 0;
        int total = 0;
        List<String> erros = new ArrayList<>();

        // Remove BOM (CSV UTF-8 do Excel) e separa em linhas.
        String conteudo = csv == null ? "" : csv.replace(BOM, "");
        String[] linhas = conteudo.split("\\r?\\n");
        boolean primeiraNaoBranca = true;

        for (int numero = 1; numero <= linhas.length; numero++) {
            String linha = linhas[numero - 1];
            if (linha.isBlank()) {
                continue;
            }
            // Cabeçalho: na primeira linha não-branca (tolera BOM e linhas em branco iniciais).
            if (primeiraNaoBranca) {
                primeiraNaoBranca = false;
                if (linha.strip().toLowerCase().startsWith("matricula")) {
                    continue;
                }
            }
            total++;
            try {
                String[] c = linha.split(";", -1);
                if (c.length < 2) {
                    throw new IllegalArgumentException("Esperadas 2 colunas: matricula;saldoMinutos");
                }
                String matricula = c[0].trim();
                int minutos = Integer.parseInt(c[1].trim());
                if (linhaService.importar(tenantId, matricula, minutos, dataReferencia)) {
                    importados++;
                } else {
                    ignorados++;
                }
            } catch (DataIntegrityViolationException dup) {
                ignorados++; // corrida no índice único -> já existe MIGRACAO (idempotente)
            } catch (Exception e) {
                erros.add("Linha " + numero + ": " + e.getMessage());
            }
        }
        return new MigracaoSaldoResponse(total, importados, ignorados, erros);
    }
}
