package br.gov.ponto.publico;

import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.publico.api.TransparenciaResponse;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.tenant.TenantRepository;
import br.gov.ponto.tenant.domain.Tenant;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Transparência ativa (12.3.6): resolve o ente pelo slug público e devolve a frequência
 * AGREGADA (sem dado pessoal) da competência. Sem autenticação; o ente é identificado pelo
 * slug e o contexto de tenant é definido só para a agregação.
 */
@Service
public class TransparenciaService {

    private final TenantRepository tenantRepository;
    private final ServidorRepository servidorRepository;
    private final VinculoRepository vinculoRepository;
    private final RegistroPontoRepository registroRepository;

    public TransparenciaService(TenantRepository tenantRepository, ServidorRepository servidorRepository,
                                VinculoRepository vinculoRepository, RegistroPontoRepository registroRepository) {
        this.tenantRepository = tenantRepository;
        this.servidorRepository = servidorRepository;
        this.vinculoRepository = vinculoRepository;
        this.registroRepository = registroRepository;
    }

    public TransparenciaResponse publico(String slug, YearMonth competencia) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ente não encontrado"));
        try {
            UUID tenantId = tenant.getId();
            TenantContext.set(tenantId.toString());
            Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);
            int servidores = servidorRepository.findByTenantId(tenantId).size();
            int vinculos = vinculoRepository.findByTenantId(tenantId).size();
            long registros = registroRepository
                    .countByTenantIdAndDataHoraServidorBetween(tenantId, periodo[0], periodo[1]);
            return new TransparenciaResponse(tenant.getNome(), competencia.toString(),
                    servidores, vinculos, registros);
        } finally {
            TenantContext.clear();
        }
    }
}
