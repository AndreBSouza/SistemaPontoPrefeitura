package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.api.ServidorResponse;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.EventoRepService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServidorService {

    private final ServidorRepository servidorRepository;
    private final VinculoRepository vinculoRepository;
    private final EventoRepService eventoRepService;

    public ServidorService(ServidorRepository servidorRepository, VinculoRepository vinculoRepository,
                           EventoRepService eventoRepService) {
        this.servidorRepository = servidorRepository;
        this.vinculoRepository = vinculoRepository;
        this.eventoRepService = eventoRepService;
    }

    @Transactional
    public ServidorResponse criar(CriarServidorRequest request) {
        UUID tenantId = TenantContext.requireCurrent();
        if (servidorRepository.existsByTenantIdAndCpf(tenantId, request.cpf())) {
            throw new ConflitoException("Ja existe servidor com o CPF " + request.cpf());
        }
        Servidor servidor = servidorRepository.save(
                new Servidor(tenantId, request.cpf(), request.nome(), request.email()));

        List<Vinculo> vinculos = new ArrayList<>();
        if (request.vinculos() != null) {
            for (CriarVinculoRequest v : request.vinculos()) {
                if (vinculoRepository.existsByTenantIdAndMatricula(tenantId, v.matricula())) {
                    throw new ConflitoException("Matricula ja existente: " + v.matricula());
                }
                Vinculo vinculo = new Vinculo(tenantId, servidor.getId(), v.matricula(), v.regime());
                vinculo.setCargo(v.cargo());
                vinculo.setCargaHorariaSemanal(v.cargaHorariaSemanal());
                vinculo.setLotacaoId(v.lotacaoId());
                vinculos.add(vinculoRepository.save(vinculo));
            }
        }
        // Inclusão de empregado no REP: vira o registro tipo "5" do AFD (Anexo IX, item 6.3).
        eventoRepService.empregadoIncluido(servidor.getCpf(), servidor.getNome(), null);
        return ServidorResponse.from(servidor, vinculos);
    }

    @Transactional
    public void lotarVinculo(UUID vinculoId, UUID lotacaoId) {
        UUID tenantId = TenantContext.requireCurrent();
        Vinculo vinculo = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vinculo inexistente"));
        vinculo.setLotacaoId(lotacaoId);
        vinculoRepository.save(vinculo);
    }

    @Transactional(readOnly = true)
    public List<ServidorResponse> listar() {
        UUID tenantId = TenantContext.requireCurrent();
        return servidorRepository.findByTenantId(tenantId).stream()
                .map(s -> ServidorResponse.from(s, vinculoRepository.findByServidorIdAndTenantId(s.getId(), tenantId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ServidorResponse> buscar(UUID id) {
        UUID tenantId = TenantContext.requireCurrent();
        return servidorRepository.findByIdAndTenantId(id, tenantId)
                .map(s -> ServidorResponse.from(s, vinculoRepository.findByServidorIdAndTenantId(s.getId(), tenantId)));
    }
}
