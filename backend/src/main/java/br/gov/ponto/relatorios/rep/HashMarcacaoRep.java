package br.gov.ponto.relatorios.rep;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Código hash (SHA-256) da marcação de ponto do REP-P.
 *
 * <p>É o campo 8 do registro tipo "7" do AFD e, por força do art. 79, VIII, também precisa aparecer
 * no Comprovante de Registro de Ponto do Trabalhador. Por isso a fórmula mora AQUI, num lugar só:
 * o valor gravado na batida tem de ser idêntico ao que sai no arquivo entregue à fiscalização —
 * um auditor que compare comprovante e AFD não pode encontrar hashes diferentes.</p>
 *
 * <p>Entrada do hash, na ordem definida pelo leiaute: NSR, tipo do registro, data/hora da marcação,
 * CPF, data/hora da gravação, identificador do coletor, indicador on-line/off-line e o
 * <b>hash do registro anterior</b> (encadeamento).</p>
 */
public final class HashMarcacaoRep {

    private HashMarcacaoRep() {
    }

    /**
     * Calcula o hash da marcação.
     *
     * @param hashAnterior hash do registro anterior do ente; vazio quando é o primeiro
     */
    public static String calcular(long nsr, Instant dataHoraMarcacao, String cpf,
                                  Instant dataHoraGravacao, MontadorAfd.Coletor coletor,
                                  boolean offline, String hashAnterior) {
        String entrada = CampoLeiaute.n(nsr, 9)
                + "7"
                + CampoLeiaute.dh(dataHoraMarcacao)
                + CampoLeiaute.n(cpf, 12)
                + CampoLeiaute.dh(dataHoraGravacao)
                + coletor.codigo()
                + (offline ? "1" : "0")
                + (hashAnterior == null ? "" : hashAnterior);
        return sha256(entrada);
    }

    private static String sha256(String texto) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(CampoLeiaute.CHARSET));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
