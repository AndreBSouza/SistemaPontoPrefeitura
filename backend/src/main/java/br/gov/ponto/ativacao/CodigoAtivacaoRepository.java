package br.gov.ponto.ativacao;

import br.gov.ponto.ativacao.domain.CodigoAtivacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CodigoAtivacaoRepository extends JpaRepository<CodigoAtivacao, UUID> {

    /** Busca global pelo hash (a ativacao ocorre sem tenant no contexto). */
    Optional<CodigoAtivacao> findByCodigoHash(String codigoHash);
}
