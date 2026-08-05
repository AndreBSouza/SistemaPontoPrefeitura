package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.domain.Lotacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LotacaoRepository extends JpaRepository<Lotacao, UUID> {

    List<Lotacao> findByTenantId(UUID tenantId);

    Optional<Lotacao> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Lotacao> findByTenantIdAndSigla(UUID tenantId, String sigla);

    List<Lotacao> findByTenantIdAndChefiaServidorId(UUID tenantId, UUID chefiaServidorId);

    List<Lotacao> findByTenantIdAndChefiaServidorIdIn(UUID tenantId, Collection<UUID> chefiaServidorIds);
}
