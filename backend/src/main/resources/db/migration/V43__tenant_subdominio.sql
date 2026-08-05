-- V43: subdomínio do ente (12.2.5). O app/login se autoconfigura pelo subdomínio (resolução
-- app-level). O apontamento DNS em si é infraestrutura (fora do código). Único por ente.

alter table tenant add column subdominio varchar(60);
create unique index uq_tenant_subdominio on tenant (subdominio) where subdominio is not null;
