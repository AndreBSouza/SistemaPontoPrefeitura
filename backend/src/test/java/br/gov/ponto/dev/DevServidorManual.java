package br.gov.ponto.dev;

import br.gov.ponto.tenant.TenantRepository;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * NÃO é um teste automatizado: sobe o app REAL na porta 8080 com um Postgres EMBARCADO (Zonky) e o
 * perfil {@code dev} (que dispara o {@code DevDataSeeder}), garante o ente "demo" e então BLOQUEIA
 * para servir — assim dá para testar o front (Vite proxy → 8080) SEM Docker/Postgres externo.
 *
 * <p>Rode só sob demanda: {@code mvn test -Dtest=DevServidorManual}. O nome sem o sufixo {@code Test}
 * faz o Surefire NÃO executá-lo no build normal. Encerre com kill/Ctrl-C.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"server.port=8080"})
@ActiveProfiles("dev")
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.NEVER)
class DevServidorManual {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void servir() throws InterruptedException {
        UUID tenantId = tenantRepository.findBySlug("demo")
                .map(t -> t.getId())
                .orElseGet(() -> tenantService.criar(
                        new CriarTenantRequest("Prefeitura Demo", "demo", TipoPoder.EXECUTIVO)).id());
        System.out.println("\n>>>>> DEV_TENANT_ID=" + tenantId + " <<<<<\n");
        new CountDownLatch(1).await(); // bloqueia: o servidor fica no ar até o processo ser morto
    }
}
