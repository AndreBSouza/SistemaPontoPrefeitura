package br.gov.ponto.cadastro.api;

import br.gov.ponto.common.validation.CpfValido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CriarServidorRequest(
        @NotBlank @CpfValido String cpf,
        @NotBlank String nome,
        @Email String email,
        @Valid List<CriarVinculoRequest> vinculos
) {
}
