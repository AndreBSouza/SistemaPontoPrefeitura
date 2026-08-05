package br.gov.ponto.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida CPF: 11 digitos e digitos verificadores corretos. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CpfValidator.class)
public @interface CpfValido {

    String message() default "CPF invalido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
