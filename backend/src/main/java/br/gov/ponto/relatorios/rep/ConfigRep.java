package br.gov.ponto.relatorios.rep;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Identificação do REP-P e do seu desenvolvedor — dados do FORNECEDOR do software (não do ente),
 * por isso ficam em configuração da aplicação e não no banco por tenant.
 *
 * <p>O número de registro no INPI é exigido pelo art. 91 (o REP-P precisa de certificado de
 * registro de programa de computador) e vai DENTRO do AFD (cabeçalho, campo 7) e do AEJ
 * (registro "02"). Sem ele o arquivo não é válido.</p>
 */
@Component
public class ConfigRep {

    private final String inpi;
    private final String desenvolvedorCnpj;
    private final String desenvolvedorNome;
    private final String desenvolvedorEmail;
    private final String ptrpNome;
    private final String ptrpVersao;

    public ConfigRep(@Value("${rep.inpi:}") String inpi,
                     @Value("${rep.desenvolvedor.cnpj:}") String desenvolvedorCnpj,
                     @Value("${rep.desenvolvedor.nome:}") String desenvolvedorNome,
                     @Value("${rep.desenvolvedor.email:}") String desenvolvedorEmail,
                     @Value("${ptrp.nome:Ponto Municipal}") String ptrpNome,
                     @Value("${ptrp.versao:1.0.0}") String ptrpVersao) {
        this.inpi = inpi;
        this.desenvolvedorCnpj = desenvolvedorCnpj;
        this.desenvolvedorNome = desenvolvedorNome;
        this.desenvolvedorEmail = desenvolvedorEmail;
        this.ptrpNome = ptrpNome;
        this.ptrpVersao = ptrpVersao;
    }

    /**
     * Recusa a emissão quando o REP-P ainda não tem registro no INPI — melhor bloquear do que
     * entregar à fiscalização um arquivo inválido com o campo zerado.
     */
    public String exigirInpi() {
        if (inpi == null || inpi.isBlank()) {
            throw new IllegalStateException(
                    "REP-P sem número de registro no INPI (art. 91 da Portaria MTP 671/2021). "
                            + "Configure 'rep.inpi' antes de emitir AFD/AEJ.");
        }
        return inpi;
    }

    public String desenvolvedorCnpj() {
        return desenvolvedorCnpj;
    }

    public String desenvolvedorNome() {
        return desenvolvedorNome;
    }

    public String desenvolvedorEmail() {
        return desenvolvedorEmail;
    }

    public String ptrpNome() {
        return ptrpNome;
    }

    public String ptrpVersao() {
        return ptrpVersao;
    }
}
