-- V18: hash de integridade encadeado dos registros de ponto (tamper-evidence / REP-P).
-- hash = SHA-256(hash_anterior + campos imutaveis do registro). Colunas nullable
-- (registros pre-existentes nao tem cadeia; novos passam a ter).

alter table registro_ponto add column hash           varchar(64);
alter table registro_ponto add column hash_anterior  varchar(64);
