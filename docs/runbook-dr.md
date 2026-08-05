# Runbook — Disaster Recovery (DR) do PostgreSQL

Procedimentos de **backup, restauração PITR (Point-In-Time Recovery)** e **teste de DR**
do banco de dados do Ponto Municipal.

> Banco: **PostgreSQL 16** (multi-tenant por RLS). Nuvem alvo: **AWS sa-east-1**
> (São Paulo), com cópia cruzada em **us-east-2** apenas para os backups (não dados quentes,
> por exigência de residência de dados no Brasil — ver `docs/dpia-ripd.md`).

---

## 1. Objetivos (RPO / RTO)

| Métrica | Alvo | Como é alcançado |
|---|---|---|
| **RPO** (perda máxima de dados) | **≤ 5 min** | Arquivamento contínuo de WAL para S3 (a cada `archive_timeout=300`s ou segmento cheio) |
| **RTO** (tempo até voltar a operar) | **≤ 1 h** | Restore automatizado de base backup + replay de WAL; runbook ensaiado |
| Retenção de backups | **35 dias** PITR + snapshots mensais por **5 anos** | Lifecycle no bucket S3 + Glacier |
| Janela de manutenção | Dom 02:00–04:00 BRT | Backups full agendados nessa janela |

A marcação de ponto é **dado de obrigação legal** (IN 008/2021 TCM-GO): perda é inaceitável.
Em incidente, **priorizar integridade** sobre disponibilidade — nunca aceitar marcações duplicadas
ou silenciosamente perdidas.

---

## 2. Topologia de backup

```
Primário (RDS/Aurora PostgreSQL, sa-east-1)
   ├── Snapshots automáticos diários (RDS) -> retenção 35d
   ├── WAL contínuo -> bucket s3://ponto-backups-saeast1/wal/ (SSE-KMS)
   ├── pg_dump lógico semanal (por schema/tenant) -> s3://ponto-backups-saeast1/dump/
   └── Replicação cross-region dos OBJETOS S3 -> s3://ponto-backups-useast2/ (DR de bucket)
```

Componentes:
- **Base backup físico:** `pgBackRest` (recomendado) ou snapshot RDS. Permite PITR.
- **WAL archive:** `archive_command` envia segmentos para o S3 cifrado (SSE-KMS, chave dedicada).
- **Dump lógico:** `pg_dump` por banco/tenant — usado para restauração granular de 1 ente
  sem afetar os demais.

### Parâmetros relevantes (postgresql.conf / parameter group)
```
wal_level = replica
archive_mode = on
archive_timeout = 300          # força fechamento de WAL a cada 5 min (RPO)
archive_command = 'pgbackrest --stanza=ponto archive-push %p'
max_wal_senders = 10
```

---

## 3. Backup — execução e verificação

### 3.1 Base backup (full) — semanal
```bash
pgbackrest --stanza=ponto --type=full backup
```

### 3.2 Backup incremental — diário
```bash
pgbackrest --stanza=ponto --type=incr backup
```

### 3.3 Dump lógico por tenant (restauração granular)
```bash
# Exporta apenas o schema de um ente (multi-tenant por RLS no schema public,
# filtrando por tenant_id; ajuste conforme a estratégia adotada).
pg_dump "$DATABASE_URL" \
  --format=custom --compress=9 \
  --file="ponto_${TENANT}_$(date +%Y%m%d).dump"
aws s3 cp "ponto_${TENANT}_$(date +%Y%m%d).dump" \
  "s3://ponto-backups-saeast1/dump/${TENANT}/" --sse aws:kms
```

### 3.4 Verificação diária (obrigatória)
- `pgbackrest --stanza=ponto check` deve passar (valida archive + repositório).
- Conferir no S3 que o WAL mais recente tem **< 10 min** de idade.
- Alarme CloudWatch: ausência de novo segmento WAL por **> 15 min** -> PagerDuty (sev2).

> **Backup que não foi restaurado não é backup.** Ver seção 6 (teste de DR).

---

## 4. Restauração PITR — passo a passo

Cenário: corrupção lógica, `DELETE`/`DROP` acidental, ou perda do primário.
Objetivo: restaurar até o instante **imediatamente anterior** ao incidente.

1. **Declarar incidente** e congelar gravações (revogar credenciais de escrita da app ou
   subir a app em modo somente-leitura / manutenção). Anotar o **timestamp do incidente** (UTC).

2. **Provisionar instância de restauração** (nova, nunca sobrescrever o primário até validar):
   ```bash
   # Restaura a base física mais próxima e prepara o replay do WAL
   pgbackrest --stanza=ponto \
     --type=time --target="2026-06-22 14:35:00+00" \
     --target-action=promote \
     --delta restore
   ```

3. **Subir o PostgreSQL** apontando para o diretório restaurado. O servidor faz o replay
   do WAL até o `recovery_target_time` e promove.

4. **Validar integridade** (ver seção 5) antes de redirecionar a aplicação.

5. **Redirecionar a aplicação:** atualizar o endpoint/secret do banco (SSM/Vault — ver
   `docs/secrets-management.md`) e reabrir gravações.

6. **Pós-incidente:** novo base backup imediato; postmortem em até 5 dias úteis.

### Restauração de **1 tenant** (sem PITR global)
Quando o dano é isolado a um ente, restaurar o dump lógico em um banco temporário e reimportar
apenas as linhas afetadas, **preservando `tenant_id` e RLS**:
```bash
pg_restore --create --dbname=postgres ponto_${TENANT}_YYYYMMDD.dump  # em instância temporária
# extrair/transferir apenas as tabelas/linhas do tenant para produção, com validação manual
```

---

## 5. Validação pós-restauração (checklist)

- [ ] `SELECT max(created_at) FROM marcacao;` coerente com o `recovery_target_time`.
- [ ] Contagem de marcações por tenant bate com o esperado (sem buracos no dia do incidente).
- [ ] **RLS ativa:** `SELECT relrowsecurity FROM pg_class WHERE relname='marcacao';` -> `t`.
- [ ] Isolamento entre tenants validado (consulta com `SET app.tenant_id` retorna só o tenant).
- [ ] Trilha de auditoria íntegra (hash chain / sequência sem lacunas).
- [ ] `actuator/health` da app verde após reconfigurar o endpoint.
- [ ] Migrations (Flyway/Liquibase) na versão esperada — sem migration pendente/parcial.

---

## 6. Teste de DR (game day)

Frequência: **trimestral** + após qualquer mudança no esquema de backup.

Roteiro:
1. Selecionar um `recovery_target_time` arbitrário (ex.: ontem 10:00 UTC).
2. Restaurar em ambiente isolado (conta/VPC de DR), **sem tocar produção**.
3. Cronometrar do início do restore até app saudável -> comparar com **RTO ≤ 1 h**.
4. Medir a defasagem do último WAL aplicável -> comparar com **RPO ≤ 5 min**.
5. Rodar a checklist da seção 5.
6. Registrar resultado (tempo real, falhas, ações) em `docs/` ou ticket; ajustar runbook.

Critério de aprovação: RTO e RPO dentro do alvo **e** checklist 100% verde.

---

## 7. Contatos e escalonamento

| Papel | Responsabilidade |
|---|---|
| On-call DBA | Executa restore, decide PITR vs dump |
| SRE on-call | Reconfigura endpoint/secrets, abre/fecha manutenção |
| DPO/Encarregado | Avalia notificação à ANPD se houver risco a dados pessoais (LGPD art. 48) |
| Gestor do contrato (ente) | Comunicação com o município afetado |

> Documento vivo — revisar a cada game day e a cada mudança de RPO/RTO ou de provedor.
