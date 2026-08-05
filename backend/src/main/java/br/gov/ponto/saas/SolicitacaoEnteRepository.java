package br.gov.ponto.saas;

import br.gov.ponto.saas.domain.SolicitacaoEnte;
import br.gov.ponto.saas.domain.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SolicitacaoEnteRepository extends JpaRepository<SolicitacaoEnte, UUID> {

    List<SolicitacaoEnte> findByStatusOrderByCriadoEm(StatusSolicitacao status);

    boolean existsBySlugAndStatus(String slug, StatusSolicitacao status);
}
