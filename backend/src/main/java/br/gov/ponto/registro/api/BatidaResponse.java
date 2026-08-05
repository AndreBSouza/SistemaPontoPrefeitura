package br.gov.ponto.registro.api;

import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.registro.domain.TipoMarcacao;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Resposta do "botao unico" de ponto. Alem do comprovante (NSR), traz o tipo
 * deduzido e uma mensagem amigavel em linguagem simples para exibir em letras
 * grandes ao servidor (ex.: "Entrada registrada às 08:03").
 */
public record BatidaResponse(
        UUID id,
        long nsr,
        UUID vinculoId,
        TipoMarcacao tipo,
        String rotuloTipo,
        OrigemRegistro origem,
        Instant dataHoraServidor,
        String horaLocal,
        String mensagem,
        boolean offline,
        boolean foraDaCerca
) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public static BatidaResponse from(RegistroPonto r) {
        String hora = HORA.format(r.getDataHoraServidor().atZone(TempoMunicipal.ZONE));
        return new BatidaResponse(
                r.getId(), r.getNsr(), r.getVinculoId(), r.getTipo(), r.getTipo().rotulo(),
                r.getOrigem(), r.getDataHoraServidor(), hora, mensagem(r.getTipo(), hora), r.isOffline(),
                r.isForaDaCerca());
    }

    private static String mensagem(TipoMarcacao tipo, String hora) {
        return switch (tipo) {
            case ENTRADA -> "Entrada registrada às " + hora;
            case SAIDA -> "Saída registrada às " + hora;
            case INTERVALO_INICIO -> "Intervalo iniciado às " + hora;
            case INTERVALO_FIM -> "Retorno do intervalo às " + hora;
        };
    }
}
