-- V23: identidade visual (white-label) por ente — nome do app, logo e cores.
-- Tudo opcional; o sistema aplica defaults quando vazio.

alter table tenant add column nome_app      varchar(60);
alter table tenant add column logo_url      varchar(500);
alter table tenant add column cor_primaria  varchar(9);
alter table tenant add column cor_acento    varchar(9);
