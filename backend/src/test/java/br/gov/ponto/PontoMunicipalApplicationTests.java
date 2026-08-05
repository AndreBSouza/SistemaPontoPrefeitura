package br.gov.ponto;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class PontoMunicipalApplicationTests {

    @Test
    void contextLoads() {
        // Garante que o contexto Spring sobe (com Flyway e JPA sobre Postgres embarcado).
    }
}
