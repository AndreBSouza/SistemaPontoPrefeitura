package br.gov.ponto.me.api;

/**
 * Resumo "a seu favor" para o app (12.1.5): saldo de banco de horas e horas extras
 * acumuladas na semana corrente — informações positivas que reforçam a adesão.
 */
public record ResumoMeResponse(
        int saldoBancoHorasMinutos,
        int horaExtraSemanaMinutos
) {
}
