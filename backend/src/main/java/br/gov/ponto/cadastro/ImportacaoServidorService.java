package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.api.ImportacaoResultado;
import br.gov.ponto.cadastro.api.ImportacaoResultado.ErroLinha;
import br.gov.ponto.cadastro.api.ServidorResponse;
import br.gov.ponto.cadastro.api.VinculoResponse;
import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Importacao em lote de servidores via CSV. Cada linha e processada em sua propria
 * transacao (delega a {@link ServidorService#criar}, outro bean): falha em uma linha
 * nao desfaz as demais, e os erros sao reportados por numero de linha. Quando ha lotacao
 * (coluna {@code lotacaoSigla} ou o parametro {@code lotacaoIdPadrao}), o vinculo ja entra
 * lotado no orgao e herda as regras dele.
 *
 * <p>Layout CSV: {@code cpf;nome;email;matricula;regime;cargo;cargaHorariaSemanal;lotacaoSigla}
 * (as duas ultimas colunas opcionais; cabecalho opcional iniciado por "cpf").</p>
 */
@Service
public class ImportacaoServidorService {

    private final ServidorService servidorService;
    private final LotacaoRepository lotacaoRepository;

    public ImportacaoServidorService(ServidorService servidorService, LotacaoRepository lotacaoRepository) {
        this.servidorService = servidorService;
        this.lotacaoRepository = lotacaoRepository;
    }

    public ImportacaoResultado importarCsv(String csv) {
        return importarCsv(csv, null);
    }

    /**
     * @param lotacaoIdPadrao órgão em que lotar todos os importados (se a linha não trouxer
     *                        {@code lotacaoSigla}); pode ser nulo.
     */
    public ImportacaoResultado importarCsv(String csv, UUID lotacaoIdPadrao) {
        // Valida o órgão padrão UMA vez (falha rápido, sem criar servidores sem lotação).
        if (lotacaoIdPadrao != null
                && lotacaoRepository.findByIdAndTenantId(lotacaoIdPadrao, TenantContext.requireCurrent()).isEmpty()) {
            throw new RecursoNaoEncontradoException("Lotação (órgão padrão) inexistente no ente");
        }
        List<ErroLinha> erros = new ArrayList<>();
        int importados = 0;
        int total = 0;
        String[] linhas = csv == null ? new String[0] : csv.split("\\r?\\n");

        for (int numero = 1; numero <= linhas.length; numero++) {
            String linha = linhas[numero - 1];
            if (linha.isBlank()) {
                continue;
            }
            if (numero == 1 && linha.toLowerCase().startsWith("cpf")) {
                continue; // cabecalho
            }
            total++;
            try {
                String[] c = linha.split(";", -1);
                CriarServidorRequest req = parse(c);
                UUID lotacaoId = resolverLotacao(c, lotacaoIdPadrao);
                ServidorResponse servidor = servidorService.criar(req);
                if (lotacaoId != null) {
                    for (VinculoResponse v : servidor.vinculos()) {
                        servidorService.lotarVinculo(v.id(), lotacaoId);
                    }
                }
                importados++;
            } catch (Exception e) {
                erros.add(new ErroLinha(numero, e.getMessage()));
            }
        }
        return new ImportacaoResultado(total, importados, erros);
    }

    private CriarServidorRequest parse(String[] c) {
        if (c.length < 5) {
            throw new IllegalArgumentException("Esperadas ao menos 5 colunas: cpf;nome;email;matricula;regime");
        }
        String cpf = c[0].trim();
        if (!cpf.matches("\\d{11}")) {
            throw new IllegalArgumentException("CPF deve conter 11 digitos");
        }
        String nome = c[1].trim();
        String email = c[2].trim().isEmpty() ? null : c[2].trim();
        String matricula = c[3].trim();
        Regime regime = Regime.valueOf(c[4].trim().toUpperCase());
        String cargo = (c.length > 5 && !c[5].isBlank()) ? c[5].trim() : null;
        Integer carga = (c.length > 6 && !c[6].isBlank()) ? Integer.valueOf(c[6].trim()) : null;
        return new CriarServidorRequest(cpf, nome, email,
                List.of(new CriarVinculoRequest(matricula, regime, cargo, carga)));
    }

    /** Lotação da linha (coluna 8 = sigla do órgão) ou o padrão do import. */
    private UUID resolverLotacao(String[] c, UUID lotacaoIdPadrao) {
        if (c.length > 7 && !c[7].isBlank()) {
            String sigla = c[7].trim();
            Lotacao lotacao = lotacaoRepository
                    .findByTenantIdAndSigla(TenantContext.requireCurrent(), sigla)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Lotação (sigla) inexistente: " + sigla));
            return lotacao.getId();
        }
        return lotacaoIdPadrao;
    }
}
