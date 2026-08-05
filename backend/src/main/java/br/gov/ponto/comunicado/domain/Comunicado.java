package br.gov.ponto.comunicado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Comunicado oficial (broadcast) da prefeitura aos servidores. Quando {@code lotacaoId}
 * é nulo, o comunicado é geral (todos os órgãos); caso contrário, é direcionado a um órgão.
 */
@Entity
@Table(name = "comunicado")
public class Comunicado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String mensagem;

    /** Órgão (lotação) alvo; nulo = comunicado geral a todos os servidores do ente. */
    @Column(name = "lotacao_id")
    private UUID lotacaoId;

    @Column(name = "publicado_em", nullable = false, updatable = false)
    private Instant publicadoEm;

    protected Comunicado() {
    }

    public Comunicado(UUID tenantId, String titulo, String mensagem, UUID lotacaoId) {
        this.tenantId = tenantId;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.lotacaoId = lotacaoId;
    }

    @PrePersist
    void prePersist() {
        if (publicadoEm == null) {
            publicadoEm = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public UUID getLotacaoId() {
        return lotacaoId;
    }

    public boolean isGeral() {
        return lotacaoId == null;
    }

    public Instant getPublicadoEm() {
        return publicadoEm;
    }
}
