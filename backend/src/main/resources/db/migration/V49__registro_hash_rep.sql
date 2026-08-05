-- Código hash (SHA-256) da marcação exigido pelo REP-P.
--
-- É o campo 8 do registro tipo "7" do AFD e, por força do art. 79, VIII, também precisa aparecer no
-- Comprovante de Registro de Ponto do Trabalhador. Gravar na batida garante que o valor mostrado ao
-- servidor seja EXATAMENTE o mesmo entregue à fiscalização no AFD.
--
-- Diferente de registro_ponto.hash, que é a cadeia de integridade INTERNA do sistema (outra
-- fórmula, anterior a esta norma). As duas coexistem de propósito: uma atende o leiaute legal, a
-- outra o verificador de integridade próprio.
alter table registro_ponto add column hash_rep varchar(64);

comment on column registro_ponto.hash_rep is
    'SHA-256 da marcacao conforme o leiaute do AFD (campo 8 do registro tipo 7). Nulo em registros
     anteriores a esta versao — nesses casos o AFD calcula o encadeamento na geracao.';
