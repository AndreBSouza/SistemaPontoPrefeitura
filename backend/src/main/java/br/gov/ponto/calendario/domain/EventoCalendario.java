package br.gov.ponto.calendario.domain;

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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento do calendário oficial do município (feriado, ponto facultativo, abono coletivo).
 * Quando {@code lotacaoId} é nulo, vale para todo o ente; caso contrário, só para o órgão.
 * Em todos os casos a data é tratada como dia não útil na apuração (não gera falta).
 */
@Entity
@Table(name = "evento_calendario")
public class EventoCalendario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEventoCalendario tipo;

    @Column(nullable = false, length = 200)
    private String descricao;

    /** Órgão (lotação) alvo; nulo = vale para todo o ente. */
    @Column(name = "lotacao_id")
    private UUID lotacaoId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected EventoCalendario() {
    }

    public EventoCalendario(UUID tenantId, LocalDate data, TipoEventoCalendario tipo,
                            String descricao, UUID lotacaoId) {
        this.tenantId = tenantId;
        this.data = data;
        this.tipo = tipo;
        this.descricao = descricao;
        this.lotacaoId = lotacaoId;
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

    public LocalDate getData() {
        return data;
    }

    public TipoEventoCalendario getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public UUID getLotacaoId() {
        return lotacaoId;
    }

    public boolean isGeral() {
        return lotacaoId == null;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
