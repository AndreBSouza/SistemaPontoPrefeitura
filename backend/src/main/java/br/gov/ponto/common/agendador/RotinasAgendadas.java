package br.gov.ponto.common.agendador;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.notificacao.LembretePendenciaService;
import br.gov.ponto.tenant.TenantRepository;
import br.gov.ponto.tenant.domain.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

/**
 * Rotinas recorrentes da operação, varrendo todos os entes (define o {@link TenantContext} por
 * ente e limpa no fim, para a RLS valer em cada iteração).
 *
 * <p>DESLIGADO por padrão — habilite com {@code ponto.agendador.enabled=true}. Pode ficar ligado em
 * TODAS as réplicas: a {@link TravaDistribuida} garante que apenas uma execute cada disparo (com
 * Redis configurado). O cron é configurável. Expurgo/retenção LGPD entra aqui quando a política de
 * retenção (decisão jurídica) estiver definida.</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "ponto.agendador", name = "enabled", havingValue = "true")
public class RotinasAgendadas {

    private static final Logger log = LoggerFactory.getLogger(RotinasAgendadas.class);

    private final TenantRepository tenantRepository;
    private final LembretePendenciaService lembreteService;
    private final TravaDistribuida trava;

    public RotinasAgendadas(TenantRepository tenantRepository, LembretePendenciaService lembreteService,
                            TravaDistribuida trava) {
        this.tenantRepository = tenantRepository;
        this.lembreteService = lembreteService;
        this.trava = trava;
    }

    /** Lembretes de ciência do espelho fechado. Padrão: dias úteis às 8h (fuso do município). */
    @Scheduled(cron = "${ponto.agendador.lembretes-cron:0 0 8 * * MON-FRI}", zone = "America/Sao_Paulo")
    public void lembretesDeCienciaPendente() {
        // Só uma réplica dispara: sem a trava, 3 réplicas = 3 notificações para cada servidor.
        if (!trava.assumir("lembretes-ciencia", Duration.ofMinutes(30))) {
            return;
        }
        for (Tenant t : tenantRepository.findAll()) {
            try {
                TenantContext.set(t.getId().toString());
                int enviados = lembreteService.lembrarCienciasPendentes();
                if (enviados > 0) {
                    log.info("Lembretes de ciência enviados no ente {}: {}", t.getSlug(), enviados);
                }
            } catch (RuntimeException e) {
                log.warn("Falha nos lembretes do ente {} ({}).", t.getId(), e.getClass().getSimpleName());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
