package br.gov.ponto.registro;

import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.api.BaterPontoRequest;
import br.gov.ponto.registro.api.BatidaResponse;
import br.gov.ponto.registro.domain.OrigemRegistro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registro no totem por matrícula (12.1.10 — inclusão de quem não tem smartphone): o servidor
 * digita a matrícula no quiosque e o totem bate o ponto (origem TOTEM, tipo deduzido).
 */
@Service
public class TotemService {

    private final VinculoRepository vinculoRepository;
    private final RegistroService registroService;

    public TotemService(VinculoRepository vinculoRepository, RegistroService registroService) {
        this.vinculoRepository = vinculoRepository;
        this.registroService = registroService;
    }

    @Transactional
    public BatidaResponse baterPorMatricula(String matricula) {
        UUID tenantId = TenantContext.requireCurrent();
        Vinculo vinculo = vinculoRepository.findByTenantIdAndMatricula(tenantId, matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula não encontrada no ente"));
        return registroService.bater(new BaterPontoRequest(vinculo.getId(), OrigemRegistro.TOTEM,
                null, null, null, false, "totem-" + UUID.randomUUID()));
    }
}
