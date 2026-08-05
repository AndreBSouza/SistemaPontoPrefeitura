package br.gov.ponto.funcionalidade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Estado de uma funcionalidade (ligada/desligada) para um ente. Único por (tenant, chave). */
@Entity
@Table(name = "funcionalidade_flag")
public class FuncionalidadeFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 40)
    private String chave;

    @Column(nullable = false)
    private boolean habilitado;

    protected FuncionalidadeFlag() {
    }

    public FuncionalidadeFlag(UUID tenantId, String chave, boolean habilitado) {
        this.tenantId = tenantId;
        this.chave = chave;
        this.habilitado = habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getChave() {
        return chave;
    }

    public boolean isHabilitado() {
        return habilitado;
    }
}
