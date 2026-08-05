package br.gov.ponto.ativacao;

import br.gov.ponto.ativacao.domain.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispositivoRepository extends JpaRepository<Dispositivo, UUID> {

    /** Autenticacao: busca global por token de dispositivo ativo (sem tenant no contexto). */
    Optional<Dispositivo> findByTokenHashAndAtivoTrue(String tokenHash);

    List<Dispositivo> findByTenantIdAndVinculoIdOrderByCriadoEm(UUID tenantId, UUID vinculoId);

    Optional<Dispositivo> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Dispositivos ativos do ente — usado nos indicadores de adesão. */
    long countByTenantIdAndAtivoTrue(UUID tenantId);

    /** Dispositivos ativos do ente — para mapear quais vínculos aderiram (adesão por órgão/regime). */
    List<Dispositivo> findByTenantIdAndAtivoTrue(UUID tenantId);
}
