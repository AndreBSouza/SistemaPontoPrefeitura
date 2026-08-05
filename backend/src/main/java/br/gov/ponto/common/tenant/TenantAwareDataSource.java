package br.gov.ponto.common.tenant;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * DataSource que define a GUC {@code app.current_tenant} em cada conexao obtida,
 * a partir do {@link TenantContext}. As policies de RLS no PostgreSQL usam essa
 * GUC para isolar os dados por ente. Em toda obtencao de conexao a GUC e
 * (re)definida ou resetada, evitando vazamento entre conexoes do pool.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return apply(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return apply(super.getConnection(username, password));
    }

    private Connection apply(Connection connection) throws SQLException {
        UUID tenant = parse(TenantContext.get());
        try (Statement st = connection.createStatement()) {
            if (tenant != null) {
                // tenant e um UUID validado -> seguro interpolar
                st.execute("SET app.current_tenant = '" + tenant + "'");
            } else {
                st.execute("RESET app.current_tenant");
            }
        }
        return connection;
    }

    private static UUID parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
