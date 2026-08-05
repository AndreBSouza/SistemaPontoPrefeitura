-- V36: sigla do órgão única por ente. Garante que findByTenantIdAndSigla (usado na importação
-- por órgão) sempre resolva no máximo um registro — evita IncorrectResultSizeDataAccessException.
-- Índice único parcial (siglas nulas não colidem entre si).

create unique index uq_lotacao_tenant_sigla on lotacao (tenant_id, sigla) where sigla is not null;
