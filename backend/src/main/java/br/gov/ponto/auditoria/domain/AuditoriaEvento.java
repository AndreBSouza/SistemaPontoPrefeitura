package br.gov.ponto.auditoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Evento de auditoria. Imutavel: nunca atualizado nem removido. */
@Entity
@Table(name = "auditoria_evento")
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(nullable = false, length = 40)
    private String entidade;

    @Column(name = "entidade_id", length = 64)
    private String entidadeId;

    @Column(length = 120)
    private String ator;

    @Column(length = 1000)
    private String detalhe;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant ocorridoEm;

    protected AuditoriaEvento() {
    }

    public AuditoriaEvento(UUID tenantId, String acao, String entidade, String entidadeId,
                           String ator, String detalhe) {
        this.tenantId = tenantId;
        this.acao = acao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.ator = ator;
        this.detalhe = detalhe;
    }

    @PrePersist
    void prePersist() {
        if (ocorridoEm == null) {
            ocorridoEm = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAcao() {
        return acao;
    }

    public String getEntidade() {
        return entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public String getAtor() {
        return ator;
    }

    public String getDetalhe() {
        return detalhe;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }
}
