#!/usr/bin/env bash
# Backup lógico do Postgres do Ponto Municipal (pg_dump, formato custom -> pg_restore).
# Uso:  DB_HOST=... DB_USER=ponto DB_NAME=ponto PGPASSWORD=... ./backup-postgres.sh [dir_destino]
# Cron: 0 2 * * *  PGPASSWORD=... /opt/ponto/infra/scripts/backup-postgres.sh /var/backups/ponto
#
# Observação: em nuvem com Postgres gerenciado, prefira o PITR/snapshot do provedor. Este script
# é o backup lógico complementar (portável entre ambientes) e a base do teste de restore do DR.
set -euo pipefail

DEST="${1:-./backups}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-ponto}"
DB_NAME="${DB_NAME:-ponto}"
RETENCAO="${RETENCAO:-14}"

mkdir -p "$DEST"
TS="$(date +%Y%m%d-%H%M%S)"
ARQ="$DEST/ponto-${DB_NAME}-${TS}.dump"

echo "Backup de '$DB_NAME' @ $DB_HOST:$DB_PORT -> $ARQ"
pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -F c -f "$ARQ"
echo "OK ($(du -h "$ARQ" | cut -f1))"

# Retenção: mantém os N mais recentes.
ls -1t "$DEST"/ponto-"${DB_NAME}"-*.dump 2>/dev/null | tail -n +"$((RETENCAO + 1))" | xargs -r rm -f
