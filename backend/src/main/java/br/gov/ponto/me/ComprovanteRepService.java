package br.gov.ponto.me;

import br.gov.ponto.cadastro.LotacaoRepository;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.AcessoNegadoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.me.api.ComprovanteRepResponse;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.relatorios.AssinaturaService;
import br.gov.ponto.relatorios.rep.CampoLeiaute;
import br.gov.ponto.relatorios.rep.ConfigRep;
import br.gov.ponto.tenant.TenantRepository;
import br.gov.ponto.tenant.domain.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Monta o <b>Comprovante de Registro de Ponto do Trabalhador</b> (art. 79 da Portaria MTP
 * 671/2021) — o documento que o servidor pode extrair a cada marcação.
 *
 * <p>O código hash (inciso VIII) é o valor <b>gravado na batida</b>, o mesmo que sai no campo 8 do
 * registro tipo "7" do AFD: é assim que o trabalhador (ou a fiscalização) consegue conferir que a
 * marcação do comprovante é exatamente a que está no arquivo entregue ao auditor.</p>
 */
@Service
public class ComprovanteRepService {

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withZone(TempoMunicipal.ZONE);

    private final RegistroPontoRepository registroRepository;
    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;
    private final LotacaoRepository lotacaoRepository;
    private final TenantRepository tenantRepository;
    private final AssinaturaService assinaturaService;
    private final ConfigRep configRep;

    public ComprovanteRepService(RegistroPontoRepository registroRepository,
                                 VinculoRepository vinculoRepository,
                                 ServidorRepository servidorRepository,
                                 LotacaoRepository lotacaoRepository,
                                 TenantRepository tenantRepository,
                                 AssinaturaService assinaturaService,
                                 ConfigRep configRep) {
        this.registroRepository = registroRepository;
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
        this.lotacaoRepository = lotacaoRepository;
        this.tenantRepository = tenantRepository;
        this.assinaturaService = assinaturaService;
        this.configRep = configRep;
    }

    /**
     * Comprovante de uma marcação do PRÓPRIO servidor.
     *
     * @param vinculoId vínculo derivado do dispositivo autenticado
     * @param nsr       número sequencial da marcação
     */
    @Transactional(readOnly = true)
    public ComprovanteRepResponse porNsr(UUID vinculoId, long nsr) {
        UUID tenantId = TenantContext.requireCurrent();
        RegistroPonto registro = registroRepository.findByTenantIdAndNsr(tenantId, nsr)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Marcação inexistente"));
        // O servidor só extrai comprovante das próprias marcações.
        if (!registro.getVinculoId().equals(vinculoId)) {
            throw new AcessoNegadoException("Esta marcação não pertence ao seu vínculo");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente inexistente"));
        Vinculo vinculo = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo inexistente"));
        Servidor servidor = servidorRepository.findByIdAndTenantId(vinculo.getServidorId(), tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Servidor inexistente"));

        String local = vinculo.getLotacaoId() == null ? tenant.getNome()
                : lotacaoRepository.findByIdAndTenantId(vinculo.getLotacaoId(), tenantId)
                        .map(l -> l.getNome()).orElse(tenant.getNome());

        String texto = textoParaAssinar(registro, tenant, servidor);
        String assinatura = assinaturaService.assinar(texto.getBytes(CampoLeiaute.CHARSET)).orElse(null);

        return new ComprovanteRepResponse(
                ComprovanteRepResponse.TITULO,
                registro.getNsr(),
                tenant.getNome(),
                tenant.getCnpj(),
                null, // CNO/CAEPF: só quando o ente possuir o cadastro
                local,
                servidor.getNome(),
                servidor.getCpf(),
                DATA_HORA.format(registro.instanteDaMarcacao()),
                configRep.exigirInpi(),
                registro.getHashRep(),
                assinatura);
    }

    /**
     * Conteúdo assinado eletronicamente (inciso IX): abrange os dados dos incisos I a VIII, como
     * a norma exige para o comprovante impresso.
     */
    private String textoParaAssinar(RegistroPonto registro, Tenant tenant, Servidor servidor) {
        return String.join("|",
                ComprovanteRepResponse.TITULO,
                String.valueOf(registro.getNsr()),
                String.valueOf(tenant.getNome()),
                String.valueOf(tenant.getCnpj()),
                String.valueOf(servidor.getNome()),
                String.valueOf(servidor.getCpf()),
                DATA_HORA.format(registro.instanteDaMarcacao()),
                configRep.exigirInpi(),
                String.valueOf(registro.getHashRep()));
    }
}
