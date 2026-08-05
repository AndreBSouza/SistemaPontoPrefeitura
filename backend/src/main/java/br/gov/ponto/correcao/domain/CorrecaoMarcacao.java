package br.gov.ponto.correcao.domain;

import br.gov.ponto.registro.domain.TipoMarcacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Solicitação de correção de marcação ("esqueci de bater" do servidor, ou correção do RH).
 * Ao ser aprovada, gera uma NOVA marcação encadeada (origem AJUSTE); o registro criado
 * fica referenciado em {@code registroId}. Nunca edita um registro existente (imutabilidade).
 */
@Entity
@Table(name = "correcao_marcacao")
public class CorrecaoMarcacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    /** Momento pretendido da marcação (a apuração ordena por ele). */
    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMarcacao tipo;

    @Column(nullable = false, length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusCorrecao status;

    /** Registro de ponto criado quando a correção é aprovada. */
    @Column(name = "registro_id")
    private UUID registroId;

    @Column(name = "motivo_decisao", length = 500)
    private String motivoDecisao;

    @Column(name = "solicitado_em", nullable = false, updatable = false)
    private Instant solicitadoEm;

    @Column(name = "decidido_em")
    private Instant decididoEm;

    protected CorrecaoMarcacao() {
    }

    public CorrecaoMarcacao(UUID tenantId, UUID vinculoId, Instant dataHora,
                            TipoMarcacao tipo, String motivo) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.dataHora = dataHora;
        this.tipo = tipo;
        this.motivo = motivo;
        this.status = StatusCorrecao.PENDENTE;
    }

    @PrePersist
    void prePersist() {
        if (solicitadoEm == null) {
            solicitadoEm = Instant.now();
        }
    }

    public void aprovar(String motivoDecisao, UUID registroId) {
        this.status = StatusCorrecao.APROVADA;
        this.registroId = registroId;
        this.motivoDecisao = motivoDecisao;
        this.decididoEm = Instant.now();
    }

    public void rejeitar(String motivoDecisao) {
        this.status = StatusCorrecao.REJEITADA;
        this.motivoDecisao = motivoDecisao;
        this.decididoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getVinculoId() {
        return vinculoId;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public TipoMarcacao getTipo() {
        return tipo;
    }

    public String getMotivo() {
        return motivo;
    }

    public StatusCorrecao getStatus() {
        return status;
    }

    public UUID getRegistroId() {
        return registroId;
    }

    public String getMotivoDecisao() {
        return motivoDecisao;
    }

    public Instant getSolicitadoEm() {
        return solicitadoEm;
    }

    public Instant getDecididoEm() {
        return decididoEm;
    }
}
