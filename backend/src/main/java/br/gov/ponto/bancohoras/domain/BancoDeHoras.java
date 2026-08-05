package br.gov.ponto.bancohoras.domain;

import br.gov.ponto.common.error.ConflitoException;

/**
 * Invariantes do banco de horas (POJO de dominio, sem Spring/JPA): respeito ao limite
 * de acumulo no credito e validacao de saldo na compensacao. O service cuida do I/O
 * (carregar saldo, persistir lancamento) e delega a decisao a esta classe.
 */
public final class BancoDeHoras {

    private final int saldoAtual;
    private final int limite;

    private BancoDeHoras(int saldoAtual, int limite) {
        this.saldoAtual = saldoAtual;
        this.limite = limite;
    }

    public static BancoDeHoras de(int saldoAtual, int limite) {
        return new BancoDeHoras(saldoAtual, limite);
    }

    /** Lancamento liquido da apuracao (credito - debito): se positivo, e clampado ao teto. */
    public int aplicarLiquido(int net) {
        if (net <= 0) {
            return net; // debito (ou zero) passa direto
        }
        int espaco = Math.max(0, limite - saldoAtual);
        return Math.min(net, espaco);
    }

    /** Valida uma compensacao; lanca se invalida. */
    public void validarCompensacao(int minutos) {
        if (minutos <= 0) {
            throw new IllegalArgumentException("minutos deve ser positivo");
        }
        if (saldoAtual < minutos) {
            throw new ConflitoException("Saldo insuficiente para compensacao");
        }
    }

    public int saldoAtual() {
        return saldoAtual;
    }
}
