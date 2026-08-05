package br.gov.ponto.jornada.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

/** Horario esperado de uma jornada em um dia da semana (ISO: 1=seg ... 7=dom). */
@Entity
@Table(name = "jornada_horario")
public class JornadaHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "jornada_id", nullable = false)
    private UUID jornadaId;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "hora_saida", nullable = false)
    private LocalTime horaSaida;

    protected JornadaHorario() {
    }

    public JornadaHorario(UUID tenantId, UUID jornadaId, int diaSemana,
                          LocalTime horaEntrada, LocalTime horaSaida) {
        this.tenantId = tenantId;
        this.jornadaId = jornadaId;
        this.diaSemana = diaSemana;
        this.horaEntrada = horaEntrada;
        this.horaSaida = horaSaida;
    }

    public UUID getId() {
        return id;
    }

    public UUID getJornadaId() {
        return jornadaId;
    }

    public int getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraEntrada() {
        return horaEntrada;
    }

    public LocalTime getHoraSaida() {
        return horaSaida;
    }
}
