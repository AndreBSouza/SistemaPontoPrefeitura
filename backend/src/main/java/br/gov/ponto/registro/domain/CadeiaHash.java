package br.gov.ponto.registro.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Hash de integridade encadeado dos registros de ponto (tamper-evidence — apoia o
 * REP-P/AFD): cada registro carrega o SHA-256 de (hash do anterior + seus campos
 * imutáveis). Alterar, remover ou inserir uma batida quebra o elo seguinte da cadeia.
 */
public final class CadeiaHash {

    private CadeiaHash() {
    }

    public static String calcular(String hashAnterior, UUID tenantId, UUID vinculoId, long nsr,
                                  TipoMarcacao tipo, Instant dataHoraServidor, String idempotencyKey) {
        String canonico = String.join("|",
                hashAnterior == null ? "" : hashAnterior,
                String.valueOf(tenantId),
                String.valueOf(vinculoId),
                String.valueOf(nsr),
                String.valueOf(tipo),
                String.valueOf(dataHoraServidor.toEpochMilli()),
                idempotencyKey);
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(canonico.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
