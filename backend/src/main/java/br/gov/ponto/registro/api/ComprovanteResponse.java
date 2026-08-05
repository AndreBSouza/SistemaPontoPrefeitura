package br.gov.ponto.registro.api;

import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.registro.domain.TipoMarcacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Comprovante de registro. Inclui a localização da batida (latitude/longitude) e o indicador
 * {@code foraDaCerca} para o administrador conferir onde o ponto foi registrado.
 *
 * <p>Inclui o {@code codigoHash} (SHA-256) exigido no comprovante do REP-P pelo art. 79, VIII
 * da Portaria MTP 671/2021 — é o MESMO valor que sai no campo 8 do registro tipo "7" do AFD.</p>
 *
 * <p>Traz também {@code rotuloTipo} e {@code horaLocal} (mesmo padrão do {@code BatidaResponse}):
 * o app exibe a lista "Meus comprovantes" com esses campos — sem eles, mostrava o enum cru e "--"
 * no lugar da hora, e a data do Instant UTC podia cair no dia seguinte para batidas noturnas.</p>
 */
public record ComprovanteResponse(
        UUID id,
        long nsr,
        UUID vinculoId,
        TipoMarcacao tipo,
        String rotuloTipo,
        OrigemRegistro origem,
        Instant dataHoraServidor,
        String horaLocal,
        String dataLocal,
        boolean offline,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean foraDaCerca,
        String codigoHash
) {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public static ComprovanteResponse from(RegistroPonto r) {
        var local = r.getDataHoraServidor().atZone(TempoMunicipal.ZONE);
        return new ComprovanteResponse(r.getId(), r.getNsr(), r.getVinculoId(),
                r.getTipo(), r.getTipo().rotulo(), r.getOrigem(), r.getDataHoraServidor(),
                HORA.format(local), DATA.format(local), r.isOffline(),
                r.getLatitude(), r.getLongitude(), r.isForaDaCerca(), r.getHashRep());
    }
}
