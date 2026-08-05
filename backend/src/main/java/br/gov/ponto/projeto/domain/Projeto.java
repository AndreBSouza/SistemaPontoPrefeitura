package br.gov.ponto.projeto.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Projeto/convênio/fonte de recurso para apropriação de horas (prestação de contas). */
@Entity
@Table(name = "projeto")
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 200)
    private String nome;

    /** Fonte de recurso / nº do convênio. */
    @Column(length = 120)
    private String fonte;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Projeto() {
    }

    public Projeto(UUID tenantId, String nome, String fonte) {
        this.tenantId = tenantId;
        this.nome = nome;
        this.fonte = fonte;
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

    public String getNome() {
        return nome;
    }

    public String getFonte() {
        return fonte;
    }
}
