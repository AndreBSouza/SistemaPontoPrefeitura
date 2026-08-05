package br.gov.ponto.notificacao.domain;

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

/** Notificacao enviada a um destinatario. */
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 200)
    private String destinatario;

    @Column(nullable = false, length = 200)
    private String assunto;

    @Column(length = 1000)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalNotificacao canal;

    @Column(name = "enviada_em", nullable = false, updatable = false)
    private Instant enviadaEm;

    protected Notificacao() {
    }

    public Notificacao(UUID tenantId, String destinatario, String assunto,
                       String mensagem, CanalNotificacao canal) {
        this.tenantId = tenantId;
        this.destinatario = destinatario;
        this.assunto = assunto;
        this.mensagem = mensagem;
        this.canal = canal;
    }

    @PrePersist
    void prePersist() {
        if (enviadaEm == null) {
            enviadaEm = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getAssunto() {
        return assunto;
    }

    public String getMensagem() {
        return mensagem;
    }

    public CanalNotificacao getCanal() {
        return canal;
    }

    public Instant getEnviadaEm() {
        return enviadaEm;
    }
}
