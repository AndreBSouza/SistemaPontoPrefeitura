-- V22: política de verificação na batida por órgão. null/false = ponto sem verificação
-- (apenas o botão); true = exige verificação no aparelho (biometria/PIN/desenho do SO,
-- ou facial do app quando o aparelho não tem bloqueio).

alter table lotacao add column verificacao_obrigatoria boolean;
