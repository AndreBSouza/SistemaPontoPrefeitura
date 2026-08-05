-- V32: diárias e deslocamentos (12.4.6) — novos tipos de ausência VIAGEM e CAPACITACAO
-- (deslocamento a serviço/treinamento fora do posto; não gera falta na apuração).

alter table ausencia_programada drop constraint ausencia_programada_tipo_check;
alter table ausencia_programada add constraint ausencia_programada_tipo_check
    check (tipo in ('FERIAS', 'LICENCA_MEDICA', 'LICENCA_MATERNIDADE', 'LICENCA_PATERNIDADE',
                    'LICENCA_PREMIO', 'LICENCA_NOJO', 'VIAGEM', 'CAPACITACAO', 'OUTRA'));
