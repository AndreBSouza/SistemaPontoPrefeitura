package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.domain.Servidor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServidorRepository extends JpaRepository<Servidor, UUID> {

    // Filtro de aplicacao por tenant (defesa primaria; RLS e a camada de banco).
    boolean existsByTenantIdAndCpf(UUID tenantId, String cpf);

    List<Servidor> findByTenantId(UUID tenantId);

    Optional<Servidor> findByIdAndTenantId(UUID id, UUID tenantId);
}
