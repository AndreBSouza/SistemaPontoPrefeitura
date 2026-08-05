package br.gov.ponto.registro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Operação do REP-P que NÃO é marcação de ponto, guardada no Armazenamento de Registro de Ponto
 * (ARP, Anexo IX item 6): inclusão/alteração/exclusão de empregado e eventos sensíveis.
 *
 * <p>No AFD viram os registros dos tipos <b>"5"</b> e <b>"6"</b>. O NSR vem da MESMA sequência das
 * marcações — o Anexo IX exige numeração sequencial única por estabelecimento, contada desde a
 * primeira operação do REP.</p>
 */
@Entity
@Table(name = "evento_rep")
public class EventoRep {

    /** Tipo de registro no AFD: inclusão/alteração/exclusão de empregado. */
    public static final short TIPO_EMPREGADO = 5;

    /** Tipo de registro no AFD: evento sensível do REP. */
    public static final short TIPO_EVENTO_SENSIVEL = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private long nsr;

    @Column(name = "tipo_registro", nullable = false)
    private short tipoRegistro;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    /** Tipo 5: "I" inclusão, "A" alteração, "E" exclusão. */
    @Column(length = 1)
    private String operacao;

    @Column(length = 14)
    private String cpf;

    @Column(length = 150)
    private String nome;

    @Column(name = "cpf_responsavel", length = 14)
    private String cpfResponsavel;

    /** Tipo 6: código do evento sensível ("02", "07", "08" no REP-P). */
    @Column(name = "codigo_evento", length = 2)
    private String codigoEvento;

    protected EventoRep() {
    }

    private EventoRep(UUID tenantId, long nsr, short tipoRegistro, Instant dataHora) {
        this.tenantId = tenantId;
        this.nsr = nsr;
        this.tipoRegistro = tipoRegistro;
        this.dataHora = dataHora;
    }

    /** Evento de empregado (registro tipo "5" do AFD). */
    public static EventoRep empregado(UUID tenantId, long nsr, Instant dataHora, String operacao,
                                      String cpf, String nome, String cpfResponsavel) {
        EventoRep e = new EventoRep(tenantId, nsr, TIPO_EMPREGADO, dataHora);
        e.operacao = operacao;
        e.cpf = cpf;
        e.nome = nome;
        e.cpfResponsavel = cpfResponsavel;
        return e;
    }

    /** Evento sensível do REP (registro tipo "6" do AFD). */
    public static EventoRep eventoSensivel(UUID tenantId, long nsr, Instant dataHora, String codigoEvento) {
        EventoRep e = new EventoRep(tenantId, nsr, TIPO_EVENTO_SENSIVEL, dataHora);
        e.codigoEvento = codigoEvento;
        return e;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public long getNsr() {
        return nsr;
    }

    public short getTipoRegistro() {
        return tipoRegistro;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public String getOperacao() {
        return operacao;
    }

    public String getCpf() {
        return cpf;
    }

    public String getNome() {
        return nome;
    }

    public String getCpfResponsavel() {
        return cpfResponsavel;
    }

    public String getCodigoEvento() {
        return codigoEvento;
    }
}
