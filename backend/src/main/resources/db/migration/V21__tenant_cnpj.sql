-- V21: CNPJ do ente (cabeçalho do AFD — Portaria 671/2021). Opcional até cadastro.

alter table tenant add column cnpj varchar(14);
