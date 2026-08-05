package br.gov.ponto.relatorios;

import br.gov.ponto.ativacao.DispositivoRepository;
import br.gov.ponto.ativacao.domain.Dispositivo;
import br.gov.ponto.cadastro.LotacaoRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.relatorios.api.AdesaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adesão ao ponto eletrônico: por órgão (12.1.9 — acompanhamento da implantação/piloto) e
 * por regime (12.1.8 — isonomia: comissionados/chefias também batem ponto). "Aderiu" = o
 * vínculo tem ao menos um dispositivo ativo.
 */
@Service
public class AdesaoService {

    private final VinculoRepository vinculoRepository;
    private final LotacaoRepository lotacaoRepository;
    private final DispositivoRepository dispositivoRepository;

    public AdesaoService(VinculoRepository vinculoRepository, LotacaoRepository lotacaoRepository,
                         DispositivoRepository dispositivoRepository) {
        this.vinculoRepository = vinculoRepository;
        this.lotacaoRepository = lotacaoRepository;
        this.dispositivoRepository = dispositivoRepository;
    }

    @Transactional(readOnly = true)
    public AdesaoResponse porOrgao() {
        UUID tenantId = TenantContext.requireCurrent();
        Set<UUID> aderiram = vinculosComDispositivo(tenantId);
        Map<UUID, String> orgaos = lotacaoRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Lotacao::getId, Lotacao::getNome, (a, b) -> a, LinkedHashMap::new));

        List<AdesaoResponse.Grupo> grupos = new ArrayList<>();
        Map<UUID, int[]> contagem = new LinkedHashMap<>(); // lotacaoId(null=sem órgão) -> [total, aderiram]
        for (Vinculo v : vinculoRepository.findByTenantId(tenantId)) {
            if (!v.isAtivo()) {
                continue;
            }
            int[] c = contagem.computeIfAbsent(v.getLotacaoId(), k -> new int[2]);
            c[0]++;
            if (aderiram.contains(v.getId())) {
                c[1]++;
            }
        }
        for (Map.Entry<UUID, int[]> e : contagem.entrySet()) {
            String rotulo = e.getKey() == null ? "Sem órgão" : orgaos.getOrDefault(e.getKey(), "?");
            grupos.add(grupo(e.getKey() == null ? "sem-orgao" : e.getKey().toString(), rotulo, e.getValue()));
        }
        return new AdesaoResponse(grupos);
    }

    @Transactional(readOnly = true)
    public AdesaoResponse porRegime() {
        UUID tenantId = TenantContext.requireCurrent();
        Set<UUID> aderiram = vinculosComDispositivo(tenantId);

        Map<Regime, int[]> contagem = new LinkedHashMap<>();
        for (Regime r : Regime.values()) {
            contagem.put(r, new int[2]);
        }
        for (Vinculo v : vinculoRepository.findByTenantId(tenantId)) {
            if (!v.isAtivo()) {
                continue;
            }
            int[] c = contagem.get(v.getRegime());
            c[0]++;
            if (aderiram.contains(v.getId())) {
                c[1]++;
            }
        }
        List<AdesaoResponse.Grupo> grupos = new ArrayList<>();
        for (Map.Entry<Regime, int[]> e : contagem.entrySet()) {
            grupos.add(grupo(e.getKey().name(), e.getKey().name(), e.getValue()));
        }
        return new AdesaoResponse(grupos);
    }

    private Set<UUID> vinculosComDispositivo(UUID tenantId) {
        return dispositivoRepository.findByTenantIdAndAtivoTrue(tenantId).stream()
                .map(Dispositivo::getVinculoId).collect(Collectors.toSet());
    }

    private AdesaoResponse.Grupo grupo(String chave, String rotulo, int[] c) {
        int pct = c[0] > 0 ? (int) Math.round(100.0 * c[1] / c[0]) : 0;
        return new AdesaoResponse.Grupo(chave, rotulo, c[0], c[1], pct);
    }
}
