-- V33: frequência de estagiários, temporários e terceirizados (12.4.7) — novos regimes
-- com regras próprias (as regras de ponto são por órgão/escala, independentes do regime).

alter table vinculo drop constraint vinculo_regime_check;
alter table vinculo add constraint vinculo_regime_check
    check (regime in ('ESTATUTARIO', 'CELETISTA', 'COMISSIONADO',
                      'ESTAGIARIO', 'TEMPORARIO', 'TERCEIRIZADO'));
