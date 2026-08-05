package br.gov.ponto.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import br.gov.ponto.ativacao.AtivacaoService;
import br.gov.ponto.ativacao.DeviceTokenAuthenticationFilter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Seguranca como resource server OIDC (tokens emitidos pelo Keycloak).
 * Mapeia as roles de realm do Keycloak (realm_access.roles) para authorities ROLE_*.
 *
 * <p>Para desenvolvimento local sem Keycloak, defina {@code ponto.security.open=true}
 * (ou perfil dev): toda a API fica liberada e o tenant vem do header X-Tenant-Id.
 * NUNCA habilitar em producao.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AtivacaoService ativacaoService,
            @Value("${ponto.security.open:false}") boolean open) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Login por aparelho (X-Device-Token) — vale em dev e em producao.
                .addFilterBefore(new DeviceTokenAuthenticationFilter(ativacaoService), AuthorizationFilter.class);

        if (open) {
            // Modo de desenvolvimento: API liberada (tenant via header X-Tenant-Id).
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/info", "/actuator/health/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Portal público de transparência ativa (dados abertos agregados).
                        .requestMatchers("/api/publico/**").permitAll()
                        // Ativacao do app: trocar codigo por token e' pre-autenticado (protegido pelo proprio codigo).
                        .requestMatchers(HttpMethod.POST, "/api/ativacao/ativar").permitAll()
                        // Identidade do ente pelo codigo, mostrada na tela de ativacao (pre-auth).
                        .requestMatchers(HttpMethod.GET, "/api/ativacao/branding").permitAll()
                        .requestMatchers("/api/ativacao/**").hasAnyRole("rh", "tenant-admin")
                        // Totem: registro por matricula (quiosque operado pelo ente).
                        .requestMatchers("/api/totem/**").hasAnyRole("operador", "rh", "tenant-admin")
                        // App do servidor autenticado por dispositivo: só os próprios dados.
                        .requestMatchers("/api/me/**").hasRole("servidor")
                        .requestMatchers("/api/tenants/**").hasRole("operador")
                        // A chefia PRECISA ler servidores/lotações: as telas de espelho, banco de
                        // horas, correções, férias e delegações (todas liberadas ao gestor abaixo)
                        // montam o seletor de servidor a partir daqui. Só LEITURA — o cadastro
                        // segue restrito ao RH pelo matcher seguinte.
                        .requestMatchers(HttpMethod.GET, "/api/servidores/**", "/api/lotacoes/**")
                        .hasAnyRole("gestor", "rh", "controladoria", "tenant-admin")
                        .requestMatchers("/api/servidores/**", "/api/jornadas/**", "/api/escalas/**", "/api/lotacoes/**")
                        .hasAnyRole("rh", "controladoria", "tenant-admin")
                        .requestMatchers(HttpMethod.POST, "/api/registros/**")
                        .hasAnyRole("servidor", "gestor", "rh", "tenant-admin")
                        .requestMatchers("/api/registros/**")
                        .hasAnyRole("gestor", "rh", "controladoria", "tenant-admin")
                        .requestMatchers("/api/apuracao/**")
                        .hasAnyRole("gestor", "rh", "controladoria", "tenant-admin")
                        // Decisão de justificativa: a controladoria também decide (o controller já
                        // a trata como aprovadora sem limite de alçada — 12.6.13).
                        .requestMatchers(HttpMethod.POST,
                                "/api/justificativas/*/aprovar", "/api/justificativas/*/rejeitar")
                        .hasAnyRole("gestor", "rh", "controladoria", "tenant-admin")
                        // SEM "servidor": este endpoint aceita ?vinculoId= arbitrário e devolve
                        // atestados/CID de qualquer colega do ente (dado de saúde, LGPD). O app do
                        // servidor usa /api/me/justificativas e /api/me/trilha, que derivam o
                        // vínculo do próprio dispositivo.
                        .requestMatchers("/api/justificativas/**")
                        .hasAnyRole("gestor", "rh", "controladoria", "tenant-admin")
                        .requestMatchers("/api/banco-horas/**")
                        .hasAnyRole("gestor", "rh", "controladoria", "tenant-admin")
                        .requestMatchers(HttpMethod.POST, "/api/espelho/ciencia")
                        .hasAnyRole("servidor", "gestor", "rh", "tenant-admin")
                        .requestMatchers("/api/espelho/**")
                        .hasAnyRole("gestor", "rh", "controladoria", "tenant-admin")
                        .requestMatchers("/api/auditoria/**", "/api/relatorios/**", "/api/conformidade/**")
                        .hasAnyRole("controladoria", "rh", "tenant-admin")
                        .requestMatchers("/api/lgpd/**")
                        .hasAnyRole("controladoria", "tenant-admin")
                        .requestMatchers("/api/biometria/**")
                        .hasAnyRole("servidor", "rh", "tenant-admin")
                        .requestMatchers("/api/onboarding/**").hasRole("operador")
                        .requestMatchers("/api/contratos/**").hasAnyRole("operador", "tenant-admin")
                        .requestMatchers("/api/integracoes/**").hasAnyRole("rh", "tenant-admin")
                        .requestMatchers("/api/notificacoes/**").hasAnyRole("gestor", "rh", "tenant-admin")
                        // Comunicados oficiais: a prefeitura (RH/admin) publica; servidor lê via /api/me/comunicados.
                        .requestMatchers("/api/comunicados/**").hasAnyRole("rh", "tenant-admin")
                        // Calendário oficial: feriados/pontos facultativos/abonos coletivos (RH/admin).
                        .requestMatchers("/api/calendario/**").hasAnyRole("rh", "tenant-admin")
                        // Correção de marcação (caixa da chefia/RH + correção direta em lote).
                        .requestMatchers("/api/correcoes/**").hasAnyRole("gestor", "rh", "tenant-admin")
                        // Férias e licenças (programação + cobertura da equipe).
                        .requestMatchers("/api/ausencias/**").hasAnyRole("gestor", "rh", "tenant-admin")
                        // Sobreaviso (on-call) — registro do RH/chefia.
                        .requestMatchers("/api/sobreaviso/**").hasAnyRole("gestor", "rh", "tenant-admin")
                        // Delegação de aprovação (substituto do gestor).
                        .requestMatchers("/api/delegacoes/**").hasAnyRole("gestor", "rh", "tenant-admin")
                        // Resumo da pesquisa de satisfação (RH/prefeito); servidor responde via /api/me/satisfacao.
                        .requestMatchers("/api/satisfacao/**").hasAnyRole("rh", "controladoria", "tenant-admin")
                        // Projetos/convênios + apropriação de horas (prestação de contas).
                        .requestMatchers("/api/projetos/**").hasAnyRole("rh", "controladoria", "tenant-admin")
                        .requestMatchers(HttpMethod.PUT, "/api/branding").hasAnyRole("rh", "tenant-admin")
                        .requestMatchers(HttpMethod.PUT, "/api/branding/subdominio").hasAnyRole("rh", "tenant-admin")
                        .requestMatchers(HttpMethod.PUT, "/api/branding/cnpj").hasAnyRole("rh", "tenant-admin")
                        .requestMatchers(HttpMethod.POST, "/api/branding/logo").hasAnyRole("rh", "tenant-admin")
                        .requestMatchers(HttpMethod.GET, "/api/branding")
                        .hasAnyRole("rh", "controladoria", "gestor", "tenant-admin", "operador")
                        .requestMatchers("/api/funcionalidades/**").hasAnyRole("rh", "tenant-admin")
                        // Migração/importação de dados de sistemas anteriores (onboarding).
                        .requestMatchers("/api/migracao/**").hasAnyRole("rh", "tenant-admin")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::extractRealmRoles);
        return converter;
    }

    private static Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(Object::toString)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
