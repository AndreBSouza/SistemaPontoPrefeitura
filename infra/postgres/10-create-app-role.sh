#!/bin/bash
# Cria o role de RUNTIME da aplicação (não-superusuário) para que a RLS por tenant do Postgres
# efetivamente barre acesso cross-tenant. O POSTGRES_USER (dono) roda o Flyway e cria as tabelas;
# a aplicação conecta como este role, sujeito à RLS.
#
# Roda automaticamente na PRIMEIRA inicialização do container (docker-entrypoint-initdb.d).
# Exige DB_APP_PASSWORD no ambiente do serviço postgres.
set -euo pipefail

: "${DB_APP_USER:=ponto_app}"
: "${DB_APP_PASSWORD:?defina DB_APP_PASSWORD (senha do role de aplicacao)}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
	DO \$\$
	BEGIN
	  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '${DB_APP_USER}') THEN
	    CREATE ROLE ${DB_APP_USER} LOGIN
	      NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS
	      PASSWORD '${DB_APP_PASSWORD}';
	  END IF;
	END
	\$\$;

	GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${DB_APP_USER};
	GRANT USAGE ON SCHEMA public TO ${DB_APP_USER};

	-- Tabelas/sequences que já existam neste ponto (normalmente nenhuma — o Flyway cria depois).
	GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${DB_APP_USER};
	GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ${DB_APP_USER};

	-- Tabelas/sequences FUTURAS criadas pelo dono (${POSTGRES_USER}) via Flyway ficam acessíveis
	-- ao role de aplicação — sem precisar de GRANT manual a cada migration.
	ALTER DEFAULT PRIVILEGES FOR ROLE ${POSTGRES_USER} IN SCHEMA public
	  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${DB_APP_USER};
	ALTER DEFAULT PRIVILEGES FOR ROLE ${POSTGRES_USER} IN SCHEMA public
	  GRANT USAGE, SELECT ON SEQUENCES TO ${DB_APP_USER};
EOSQL

echo "Role de aplicacao '${DB_APP_USER}' criado (NOSUPERUSER/NOBYPASSRLS) — RLS por tenant ativa."
