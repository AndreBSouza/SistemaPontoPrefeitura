package br.gov.ponto.satisfacao;

import br.gov.ponto.satisfacao.domain.PesquisaSatisfacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SatisfacaoRepository extends JpaRepository<PesquisaSatisfacao, UUID> {

    List<PesquisaSatisfacao> findByTenantId(UUID tenantId);
}
