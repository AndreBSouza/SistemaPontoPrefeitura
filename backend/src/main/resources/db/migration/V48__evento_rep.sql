-- Armazenamento de Registro de Ponto (ARP) — operações do REP-P que NÃO são marcação de ponto.
--
-- O Anexo IX (item 6) exige que o REP-P armazene, além das marcações, a inclusão/alteração/exclusão
-- de empregado e os eventos sensíveis; o AFD os expõe como registros dos tipos "5" e "6". O NSR é
-- ÚNICO por ente e compartilhado com as marcações ("numeração sequencial em incrementos unitários,
-- iniciando-se em 1 na primeira operação do REP em relação ao estabelecimento"), por isso vem do
-- mesmo nsr_sequencia usado pelo registro de ponto.
create table evento_rep (
    id               uuid primary key,
    tenant_id        uuid        not null,
    nsr              bigint      not null,
    tipo_registro    smallint    not null,          -- 5 = empregado, 6 = evento sensível
    data_hora        timestamptz not null,          -- data e hora da gravação do registro
    -- varchar (não char): char(n) vira "bpchar" no Postgres e a validação de schema do Hibernate
    -- rejeita, porque para String ela espera varchar.
    operacao         varchar(1),                    -- tipo 5: I (inclusão), A (alteração), E (exclusão)
    cpf              varchar(14),                   -- tipo 5
    nome             varchar(150),                  -- tipo 5
    cpf_responsavel  varchar(14),                   -- tipo 5
    codigo_evento    varchar(2),                    -- tipo 6: 02, 07, 08 (aplicáveis ao REP-P)
    constraint ck_evento_rep_tipo check (tipo_registro in (5, 6)),
    constraint uq_evento_rep_nsr unique (tenant_id, nsr)
);

create index ix_evento_rep_tenant_data on evento_rep (tenant_id, data_hora);

alter table evento_rep enable row level security;
alter table evento_rep force row level security;

create policy evento_rep_isolamento on evento_rep
    using (tenant_id = current_setting('app.current_tenant', true)::uuid)
    with check (tenant_id = current_setting('app.current_tenant', true)::uuid);
