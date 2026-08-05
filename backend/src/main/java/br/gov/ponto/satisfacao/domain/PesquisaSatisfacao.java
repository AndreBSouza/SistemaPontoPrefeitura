package br.gov.ponto.satisfacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Avaliação de satisfação do servidor (pesquisa de aceitação do ponto eletrônico). */
@Entity
@Table(name = "pesquisa_satisfacao")
public class PesquisaSatisfacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    /** Nota de 1 (muito insatisfeito) a 5 (muito satisfeito). */
    @Column(nullable = false)
    private int nota;

    @Column(length = 500)
    private String comentario;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected PesquisaSatisfacao() {
    }

    public PesquisaSatisfacao(UUID tenantId, UUID vinculoId, int nota, String comentario) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.nota = nota;
        this.comentario = comentario;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public int getNota() {
        return nota;
    }

    public String getComentario() {
        return comentario;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
