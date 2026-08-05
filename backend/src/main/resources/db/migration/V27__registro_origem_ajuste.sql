-- V27: permite a origem AJUSTE em registro_ponto (correção "esqueci de bater" / correção do RH).
-- A marcação de correção é uma NOVA batida encadeada na cadeia de hash (registros são imutáveis).

alter table registro_ponto drop constraint registro_ponto_origem_check;
alter table registro_ponto add constraint registro_ponto_origem_check
    check (origem in ('MOBILE', 'WEB', 'TOTEM', 'AJUSTE'));
