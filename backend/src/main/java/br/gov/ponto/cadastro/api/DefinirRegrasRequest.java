package br.gov.ponto.cadastro.api;

import br.gov.ponto.cadastro.domain.RegrasPonto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Regras de ponto do orgao. Campos nulos = "nao definido" (herda default/sistema). */
public record DefinirRegrasRequest(
        UUID jornadaPadraoId,
        @Min(0) Integer toleranciaMinutos,
        Boolean bancoHorasHabilitado,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal geofenceLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal geofenceLongitude,
        @Positive Integer geofenceRaioMetros,
        @Min(0) Integer tetoBancoHorasMinutos,
        Boolean verificacaoObrigatoria,
        LocalDate adaptacaoAte,
        Boolean teletrabalho
) {
    public RegrasPonto paraDominio() {
        return new RegrasPonto(jornadaPadraoId, toleranciaMinutos, bancoHorasHabilitado,
                geofenceLatitude, geofenceLongitude, geofenceRaioMetros,
                tetoBancoHorasMinutos, verificacaoObrigatoria, adaptacaoAte, teletrabalho);
    }
}
