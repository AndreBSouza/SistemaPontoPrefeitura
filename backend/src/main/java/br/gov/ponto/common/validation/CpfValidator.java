package br.gov.ponto.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<CpfValido, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null) {
            return true; // ausencia e tratada por @NotBlank
        }
        if (!cpf.matches("\\d{11}")) {
            return false;
        }
        if (cpf.chars().distinct().count() == 1) {
            return false; // todos os digitos iguais
        }
        return digitoVerificador(cpf, 9, 10) == (cpf.charAt(9) - '0')
                && digitoVerificador(cpf, 10, 11) == (cpf.charAt(10) - '0');
    }

    private int digitoVerificador(String cpf, int quantidade, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (cpf.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
