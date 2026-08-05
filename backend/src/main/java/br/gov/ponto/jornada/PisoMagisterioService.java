package br.gov.ponto.jornada;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.api.PisoMagisterioResponse;
import br.gov.ponto.jornada.domain.Jornada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Verificação da hora-atividade do magistério (Lei do Piso 11.738/2008, 12.5.8): ao menos 1/3 da
 * carga horária deve ser hora-atividade (planejamento, fora de sala). Avalia as jornadas que
 * declararam hora-atividade e sinaliza as que não atingem o mínimo legal — para o controle interno.
 */
@Service
public class PisoMagisterioService {

    /** Mínimo legal de hora-atividade: 1/3 da carga. */
    private static final int DIVISOR_PISO = 3;

    private final JornadaRepository jornadaRepository;

    public PisoMagisterioService(JornadaRepository jornadaRepository) {
        this.jornadaRepository = jornadaRepository;
    }

    @Transactional(readOnly = true)
    public List<PisoMagisterioResponse> avaliar() {
        UUID tenantId = TenantContext.requireCurrent();
        return jornadaRepository.findByTenantId(tenantId).stream()
                .filter(j -> j.getHoraAtividadeMin() != null)
                .map(this::avaliarJornada)
                .toList();
    }

    private PisoMagisterioResponse avaliarJornada(Jornada j) {
        int carga = j.getCargaHorariaSemanalMin();
        int ha = j.getHoraAtividadeMin();
        int minimo = (int) Math.ceil(carga / (double) DIVISOR_PISO);
        boolean atende = (long) ha * DIVISOR_PISO >= carga; // ha >= carga/3, sem erro de arredondamento
        double percentual = carga == 0 ? 0 : (double) ha / carga;
        return new PisoMagisterioResponse(j.getId(), j.getNome(), carga, ha, minimo, percentual, atende);
    }
}
