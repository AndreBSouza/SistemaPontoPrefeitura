package br.gov.ponto.lgpd;

import br.gov.ponto.auditoria.AuditoriaService;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.lgpd.api.EliminacaoResponse;
import br.gov.ponto.lgpd.api.ExportacaoTitularResponse;
import br.gov.ponto.lgpd.domain.Consentimento;
import br.gov.ponto.registro.RegistroPontoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Controles LGPD: consentimento, exportacao e eliminacao de dados do titular. */
@Service
public class LgpdService {

    private final ConsentimentoRepository consentimentoRepository;
    private final ServidorRepository servidorRepository;
    private final VinculoRepository vinculoRepository;
    private final RegistroPontoRepository registroRepository;
    private final AuditoriaService auditoriaService;

    public LgpdService(ConsentimentoRepository consentimentoRepository,
                       ServidorRepository servidorRepository,
                       VinculoRepository vinculoRepository,
                       RegistroPontoRepository registroRepository,
                       AuditoriaService auditoriaService) {
        this.consentimentoRepository = consentimentoRepository;
        this.servidorRepository = servidorRepository;
        this.vinculoRepository = vinculoRepository;
        this.registroRepository = registroRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public void registrarConsentimento(UUID servidorId, String finalidade, boolean concedido) {
        UUID tenantId = TenantContext.requireCurrent();
        servidorRepository.findByIdAndTenantId(servidorId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Servidor inexistente"));
        consentimentoRepository.save(new Consentimento(tenantId, servidorId, finalidade, concedido));
    }

    @Transactional(readOnly = true)
    public boolean consentimentoVigente(UUID servidorId, String finalidade) {
        return consentimentoRepository
                .findFirstByServidorIdAndTenantIdAndFinalidadeOrderByRegistradoEmDesc(
                        servidorId, TenantContext.requireCurrent(), finalidade)
                .map(Consentimento::isConcedido)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public ExportacaoTitularResponse exportarDadosTitular(UUID servidorId) {
        UUID tenantId = TenantContext.requireCurrent();
        Servidor s = servidorRepository.findByIdAndTenantId(servidorId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Servidor inexistente"));
        List<Vinculo> vinculos = vinculoRepository.findByServidorIdAndTenantId(servidorId, tenantId);
        int totalRegistros = vinculos.stream()
                .mapToInt(v -> registroRepository
                        .findByVinculoIdAndTenantIdOrderByNsr(v.getId(), tenantId).size())
                .sum();
        return new ExportacaoTitularResponse(s.getId(), s.getNome(), s.getCpf(), s.getEmail(),
                vinculos.size(), totalRegistros);
    }

    @Transactional
    public EliminacaoResponse eliminarDadosTitular(UUID servidorId) {
        UUID tenantId = TenantContext.requireCurrent();
        Servidor s = servidorRepository.findByIdAndTenantId(servidorId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Servidor inexistente"));
        // Anonimiza dados pessoais elegiveis; CPF e registros sao retidos por obrigacao legal.
        s.setNome("[REMOVIDO LGPD]");
        s.setEmail(null);
        s.setAtivo(false);
        servidorRepository.save(s);
        auditoriaService.registrar("ELIMINACAO_LGPD", "servidor", servidorId.toString(),
                "Dados pessoais anonimizados; CPF/registros retidos por obrigacao legal");
        return new EliminacaoResponse(servidorId, "ANONIMIZADO",
                "Dados pessoais eliminados; CPF e registros retidos por obrigacao legal");
    }
}
