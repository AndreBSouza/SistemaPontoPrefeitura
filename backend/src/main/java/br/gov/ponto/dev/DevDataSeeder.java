package br.gov.ponto.dev;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.EscalaService;
import br.gov.ponto.jornada.JornadaService;
import br.gov.ponto.jornada.api.CriarEscalaRequest;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
import br.gov.ponto.jornada.api.HorarioRequest;
import br.gov.ponto.jornada.domain.TipoJornada;
import br.gov.ponto.tenant.TenantRepository;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.api.TenantResponse;
import br.gov.ponto.tenant.domain.TipoPoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Semeia dados de demonstracao no perfil dev (ente "demo" + servidor/vinculo/jornada/escala),
 * para teste local imediato do web/totem e do app. Idempotente. Nunca roda em producao/testes.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final ServidorService servidorService;
    private final JornadaService jornadaService;
    private final EscalaService escalaService;

    public DevDataSeeder(TenantService tenantService, TenantRepository tenantRepository,
                         ServidorService servidorService, JornadaService jornadaService,
                         EscalaService escalaService) {
        this.tenantService = tenantService;
        this.tenantRepository = tenantRepository;
        this.servidorService = servidorService;
        this.jornadaService = jornadaService;
        this.escalaService = escalaService;
    }

    @Override
    public void run(String... args) {
        if (tenantRepository.existsBySlug("demo")) {
            tenantRepository.findBySlug("demo").ifPresent(t ->
                    log.info("[dev-seed] ente 'demo' ja existe — X-Tenant-Id={}", t.getId()));
            return;
        }
        TenantResponse tenant = tenantService.criar(
                new CriarTenantRequest("Prefeitura Demo", "demo", TipoPoder.EXECUTIVO));
        TenantContext.set(tenant.id().toString());
        try {
            var servidor = servidorService.criar(new CriarServidorRequest(
                    "39053344705", "Servidor Demo", "demo@ente.gov.br",
                    List.of(new CriarVinculoRequest("M-DEMO", Regime.ESTATUTARIO, "Analista", 40))));
            UUID vinculoId = servidor.vinculos().get(0).id();

            UUID jornadaId = jornadaService.criar(
                    new CriarJornadaRequest("Comercial 8-17", TipoJornada.FIXA, 2400, 10, 60)).id();
            jornadaService.definirHorarios(jornadaId, List.of(
                    new HorarioRequest(LocalDate.now().getDayOfWeek().getValue(),
                            LocalTime.of(8, 0), LocalTime.of(17, 0))));
            escalaService.atribuir(new CriarEscalaRequest(
                    vinculoId, jornadaId, LocalDate.now().minusMonths(1), null));

            log.info("[dev-seed] PRONTO. Use no web/totem e no app:");
            log.info("[dev-seed]   X-Tenant-Id = {}", tenant.id());
            log.info("[dev-seed]   vinculoId   = {}", vinculoId);
        } finally {
            TenantContext.clear();
        }
    }
}
