-- V17: a referencia biometrica passa a guardar o descritor facial (embedding) usado
-- na comparacao 1:1 (similaridade de cosseno). Amplia o tamanho para caber o vetor.

alter table referencia_biometrica alter column referencia type varchar(8192);
