package br.gov.ponto.projeto;

import br.gov.ponto.projeto.domain.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {

    List<Projeto> findByTenantIdOrderByNome(UUID tenantId);

    Optional<Projeto> findByIdAndTenantId(UUID id, UUID tenantId);
}
