package br.gov.ponto.biometria;

import br.gov.ponto.biometria.api.VerificacaoResponse;
import br.gov.ponto.biometria.domain.ComparadorFacial;
import br.gov.ponto.biometria.domain.ReferenciaBiometrica;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.common.crypto.CriptografiaService;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.lgpd.LgpdService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cadastro (enrollment) da referencia biometrica do servidor. Exige consentimento
 * LGPD vigente para a finalidade BIOMETRIA.
 *
 * <p>Nota: a comparacao 1:1 face-a-face (reconhecimento) requer modelo de embeddings
 * (TFLite/face-recognition) — o ML Kit no app faz apenas deteccao + liveness. Esta
 * referencia armazena o template/hash a ser comparado quando o reconhecimento for plugado.</p>
 */
@Service
public class BiometriaService {

    private static final String FINALIDADE = "BIOMETRIA";

    private final ReferenciaBiometricaRepository referenciaRepository;
    private final ServidorRepository servidorRepository;
    private final LgpdService lgpdService;
    private final CriptografiaService criptografia;

    public BiometriaService(ReferenciaBiometricaRepository referenciaRepository,
                            ServidorRepository servidorRepository,
                            LgpdService lgpdService,
                            CriptografiaService criptografia) {
        this.referenciaRepository = referenciaRepository;
        this.servidorRepository = servidorRepository;
        this.lgpdService = lgpdService;
        this.criptografia = criptografia;
    }

    @Transactional
    public void cadastrarReferencia(UUID servidorId, String referencia) {
        UUID tenantId = TenantContext.requireCurrent();
        servidorRepository.findByIdAndTenantId(servidorId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Servidor inexistente"));
        if (!lgpdService.consentimentoVigente(servidorId, FINALIDADE)) {
            throw new ConflitoException("Consentimento de biometria ausente para o servidor");
        }
        ReferenciaBiometrica ref = referenciaRepository
                .findByServidorIdAndTenantId(servidorId, tenantId)
                .orElseGet(() -> new ReferenciaBiometrica(tenantId, servidorId, referencia));
        ref.setReferencia(criptografia.cifrar(referencia));
        referenciaRepository.save(ref);
    }

    /**
     * Verificacao 1:1: compara o descritor facial capturado com a referencia cadastrada
     * do servidor. A extracao do descritor exige modelo de reconhecimento — ver
     * {@link ComparadorFacial}; aqui fica a comparacao + o limiar.
     */
    @Transactional(readOnly = true)
    public VerificacaoResponse verificar(UUID servidorId, String descritorCandidato) {
        UUID tenantId = TenantContext.requireCurrent();
        ReferenciaBiometrica ref = referenciaRepository.findByServidorIdAndTenantId(servidorId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Referencia biometrica nao cadastrada"));
        double similaridade = ComparadorFacial.similaridade(
                ComparadorFacial.parse(criptografia.decifrar(ref.getReferencia())),
                ComparadorFacial.parse(descritorCandidato));
        return new VerificacaoResponse(similaridade >= ComparadorFacial.LIMIAR_PADRAO,
                similaridade, ComparadorFacial.LIMIAR_PADRAO);
    }
}
