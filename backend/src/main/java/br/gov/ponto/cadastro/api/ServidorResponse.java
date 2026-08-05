package br.gov.ponto.cadastro.api;

import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;

import java.util.List;
import java.util.UUID;

public record ServidorResponse(
        UUID id,
        String cpf,
        String nome,
        String email,
        boolean ativo,
        List<VinculoResponse> vinculos
) {
    public static ServidorResponse from(Servidor s, List<Vinculo> vinculos) {
        return new ServidorResponse(s.getId(), s.getCpf(), s.getNome(), s.getEmail(), s.isAtivo(),
                vinculos.stream().map(VinculoResponse::from).toList());
    }
}
