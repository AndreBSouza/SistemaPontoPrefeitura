package br.gov.ponto.funcionalidade;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.funcionalidade.api.FuncionalidadeResponse;
import br.gov.ponto.funcionalidade.domain.Funcionalidade;
import br.gov.ponto.funcionalidade.domain.FuncionalidadeFlag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Liga/desliga funcionalidades por ente (feature flags), lido em todo o sistema. */
@Service
public class FuncionalidadeService {

    private final FuncionalidadeFlagRepository repository;

    public FuncionalidadeService(FuncionalidadeFlagRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean habilitada(Funcionalidade f) {
        return repository.findByTenantIdAndChave(TenantContext.requireCurrent(), f.name())
                .map(FuncionalidadeFlag::isHabilitado)
                .orElse(f.padrao());
    }

    @Transactional
    public void definir(String chave, boolean habilitado) {
        Funcionalidade f = Funcionalidade.valueOf(chave); // valida a chave (400 se inválida)
        UUID tenantId = TenantContext.requireCurrent();
        FuncionalidadeFlag flag = repository.findByTenantIdAndChave(tenantId, f.name())
                .orElseGet(() -> new FuncionalidadeFlag(tenantId, f.name(), habilitado));
        flag.setHabilitado(habilitado);
        repository.save(flag);
    }

    @Transactional(readOnly = true)
    public List<FuncionalidadeResponse> listar() {
        Map<String, Boolean> atuais = repository.findByTenantId(TenantContext.requireCurrent()).stream()
                .collect(Collectors.toMap(FuncionalidadeFlag::getChave, FuncionalidadeFlag::isHabilitado));
        return Arrays.stream(Funcionalidade.values())
                .map(f -> new FuncionalidadeResponse(f.name(), f.rotulo(),
                        atuais.getOrDefault(f.name(), f.padrao())))
                .toList();
    }
}
