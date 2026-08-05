package br.gov.ponto.ativacao;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Authentication de um app autenticado por token de dispositivo. Recebe a authority
 * {@code ROLE_servidor} — o mesmo papel do servidor no Keycloak — para reaproveitar
 * as regras de autorizacao existentes.
 */
public class DispositivoAuthentication extends AbstractAuthenticationToken {

    private final DispositivoPrincipal principal;

    public DispositivoAuthentication(DispositivoPrincipal principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_servidor")));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public DispositivoPrincipal getPrincipal() {
        return principal;
    }

    /** Identificador curto do ator para a trilha de auditoria (cabe em ator varchar(120)). */
    @Override
    public String getName() {
        return "vinculo:" + principal.vinculoId();
    }
}
