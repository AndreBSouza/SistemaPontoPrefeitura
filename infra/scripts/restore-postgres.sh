#!/usr/bin/env bash
# Restore do Postgres a partir de um dump (pg_restore). ATENÇÃO: SUBSTITUI os dados atuais.
# Uso:  DB_HOST=... DB_USER=ponto DB_NAME=ponto PGPASSWORD=... ./restore-postgres.sh <arquivo.dump>
# Automação (sem prompt): FORCE=1 ... ./restore-postgres.sh <arquivo.dump>
#
# Faz parte do teste de restore do DR: restaure num banco de STAGING e valide a aplicação
# (login, um relatório, verificador de integridade da cadeia) — nunca teste direto em produção.
set -euo pipefail

ARQ="${1:?informe o arquivo .dump}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-ponto}"
DB_NAME="${DB_NAME:-ponto}"

[ -f "$ARQ" ] || { echo "arquivo não encontrado: $ARQ" >&2; exit 1; }

echo "ATENÇÃO: restaurar '$ARQ' em '$DB_NAME' @ $DB_HOST:$DB_PORT — os dados atuais serão substituídos."
if [ "${FORCE:-}" != "1" ]; then
  read -r -p "Digite CONFIRMA para prosseguir: " ok
  [ "$ok" = "CONFIRMA" ] || { echo "abortado."; exit 1; }
fi

pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --clean --if-exists --no-owner "$ARQ"
echo "Restore concluído. Valide: login, um relatório e o verificador de integridade da cadeia."
