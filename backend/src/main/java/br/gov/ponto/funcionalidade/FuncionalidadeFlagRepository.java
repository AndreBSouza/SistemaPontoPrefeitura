package br.gov.ponto.funcionalidade;

import br.gov.ponto.funcionalidade.domain.FuncionalidadeFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FuncionalidadeFlagRepository extends JpaRepository<FuncionalidadeFlag, UUID> {

    Optional<FuncionalidadeFlag> findByTenantIdAndChave(UUID tenantId, String chave);

    List<FuncionalidadeFlag> findByTenantId(UUID tenantId);
}
