# Keycloak — Realm `ponto`

Configuração do provedor de identidade do **Ponto Municipal**. O arquivo
[`ponto-realm.json`](./ponto-realm.json) descreve, de forma declarativa, o realm
`ponto` com os perfis de acesso, os clients da aplicação, a política de MFA/OTP
para perfis administrativos. **Sem federação gov.br** — os administradores
autenticam pelo próprio Keycloak (usuário/senha do realm + MFA).

> Ambiente de **desenvolvimento** apenas. As credenciais e URLs aqui são para uso
> local. Não reutilize segredos deste diretório em produção.

## Sumário

- [Visão geral](#visão-geral)
- [Import do realm](#import-do-realm)
- [Perfis (realm roles)](#perfis-realm-roles)
- [Clients](#clients)
- [MFA / OTP](#mfa--otp)
- [Autenticação (sem gov.br)](#autenticação-sem-govbr)
- [Solução de problemas](#solução-de-problemas)

## Visão geral

| Item | Valor |
| --- | --- |
| Realm | `ponto` |
| Imagem | `quay.io/keycloak/keycloak:26.0` |
| Console admin | <http://localhost:8081> (usuário `admin` / senha `admin`) |
| Issuer do realm | <http://localhost:8081/realms/ponto> |
| Discovery OIDC | <http://localhost:8081/realms/ponto/.well-known/openid-configuration> |

O Keycloak roda em modo `start-dev --import-realm` (ver
[`../docker-compose.yml`](../docker-compose.yml)). O diretório `infra/keycloak`
é montado em `/opt/keycloak/data/import`, de onde os realms são importados
automaticamente no boot.

## Import do realm

### Automático (recomendado)

O `docker-compose` já importa o realm na primeira subida:

```bash
cd infra
docker compose up -d keycloak
# acompanhe o log até ver "Imported realm ponto"
docker compose logs -f keycloak
```

> O flag `--import-realm` só **cria** o realm se ele ainda não existir. Se o realm
> `ponto` já estiver no banco do Keycloak, alterações no JSON **não** são
> reaplicadas. Para reimportar do zero, veja [Reimportar](#reimportar-do-zero).

### Reimportar do zero

Como o `start-dev` usa um banco H2 efêmero dentro do container, basta recriar o
container para reimportar:

```bash
cd infra
docker compose rm -sf keycloak
docker compose up -d keycloak
```

Se você tiver apontado o Keycloak para um Postgres persistente, apague o realm
antes pelo console admin (Realm settings → Action → Delete) e suba novamente.

### Import manual (CLI)

Alternativa sem reciclar o container, útil para aplicar mudanças pontuais:

```bash
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/ponto-realm.json \
  --override true
```

> `--override true` sobrescreve o realm existente. Use com cuidado: ele substitui
> a configuração, mas **não** remove usuários já criados.

### Export (para versionar mudanças feitas pela UI)

Se você ajustar algo pelo console e quiser trazer de volta para o repositório:

```bash
docker compose exec keycloak \
  /opt/keycloak/bin/kc.sh export \
  --dir /opt/keycloak/data/import \
  --realm ponto \
  --users realm_file
```

Depois copie/limpe o JSON gerado de volta para `ponto-realm.json` (remova IDs e
segredos antes de commitar).

## Perfis (realm roles)

Os seis perfis do produto são modelados como **realm roles**:

| Role | Descrição | MFA obrigatório |
| --- | --- | :---: |
| `servidor` | Registra ponto e acompanha o próprio espelho | — |
| `gestor` | Chefia imediata: aprova justificativas da equipe | — |
| `rh` | Departamento de Pessoal: cadastro, apuração, fechamento | ✅ |
| `controladoria` | Controle Interno: auditoria e conformidade | ✅ |
| `tenant-admin` | Administrador do ente | ✅ |
| `operador` | Operador SaaS (super admin) | ✅ |

Os roles chegam ao backend dentro do claim `realm_access.roles` do access token
(client scope `roles`, já habilitado no `ponto-web`).

## Clients

| Client | Tipo | Uso |
| --- | --- | --- |
| `ponto-web` | Público (SPA), Authorization Code + **PKCE S256** | Painel web React/Vite |
| `ponto-backend` | `bearer-only` | Resource server Spring Boot |

- **`ponto-web`** — `redirectUris` e `webOrigins` apontam para
  `http://localhost:5173` (porta padrão do Vite). PKCE é exigido
  (`pkce.code.challenge.method = S256`). Por ser um client público, **não** tem
  segredo. Logout pós-redirect também aponta para `localhost:5173`.
- **`ponto-backend`** — `bearer-only`: apenas valida tokens (não inicia login nem
  possui fluxo de browser). É a audiência esperada pela API. Não tem segredo nem
  service account.

Ao adicionar a URL de produção do painel, inclua-a em `redirectUris` e
`webOrigins` do `ponto-web` (nunca use `*` em produção).

## MFA / OTP

A política de MFA exige **OTP/TOTP** (app autenticador) somente para os perfis
administrativos: **`rh`, `controladoria`, `tenant-admin` e `operador`**. Os
perfis `servidor` e `gestor` autenticam apenas com usuário e senha.

### Como está modelado

1. **Required action `CONFIGURE_TOTP`** — habilitada no realm. É ela que
   apresenta a tela de "cadastre seu app autenticador" (QR Code) quando o usuário
   ainda não tem um OTP registrado.
2. **Authentication flow `browser-mfa`** — definido como `browserFlow` (fluxo de
   login padrão do realm). Espelha o fluxo de browser nativo do Keycloak, mas no
   passo de OTP usa um sub-fluxo **CONDITIONAL**:
   - `auth-username-password-form` (REQUIRED) — usuário e senha.
   - Sub-fluxo **`browser-mfa OTP condicional admin`** (CONDITIONAL) — contém um
     sub-fluxo condicional por role administrativo (`rh`, `controladoria`,
     `tenant-admin`, `operador`). Cada um usa o executor
     `conditional-user-role`; quando a condição bate, o `auth-otp-form` vira
     REQUIRED.
   - Se o usuário tem um desses roles **e** ainda não configurou OTP, o Keycloak
     dispara automaticamente a required action `CONFIGURE_TOTP` no login.

> Por que um sub-fluxo por role? O executor `conditional-user-role` avalia **um**
> role por vez. Encadear quatro sub-fluxos CONDITIONAL produz um "OU" lógico:
> basta possuir qualquer um dos roles administrativos para o OTP ser exigido.

### Parâmetros do OTP

Definidos em `otpPolicy*` do realm: TOTP, `HmacSHA1`, 6 dígitos, janela de 30s
(compatível com Google Authenticator, Microsoft Authenticator e FreeOTP).

### Forçar o cadastro imediatamente

O OTP é solicitado no próximo login do usuário administrativo. Para forçar o
registro fora do fluxo de login, no console admin: **Users → (usuário) → Required
user actions → Configure OTP**.

### Conferir/ajustar pela UI

Console admin → **Authentication** → flow **`browser-mfa`**. Confirme que ele está
marcado como **Bound** ao "Browser flow" em **Action → Bind flow** (o import já
faz isso via `browserFlow`).

## Autenticação (sem gov.br)

O login federado **gov.br foi removido** a pedido (`identityProviders: []`). A
autenticação do produto é:

- **Servidores (app móvel):** ativação por **código gerado pelo RH** no painel; o
  aparelho recebe um **token de dispositivo** e o envia no cabeçalho
  `X-Device-Token` em toda requisição (login único por aparelho, revogável pelo
  RH). Não passa pelo Keycloak — ver o módulo `ativacao` no backend.
- **Administradores** (rh/gestor/controladoria/tenant-admin/operador): login do
  **próprio Keycloak** (usuário/senha do realm `ponto` + MFA OTP), sem federação
  externa. O backend valida o JWT do Keycloak como resource server.

> Caso um dia o gov.br seja desejado, basta cadastrar um Identity Provider OIDC
> `govbr` no realm (Keycloak faz o brokering); nada no app/backend precisa mudar.

## Solução de problemas

| Sintoma | Causa provável / ação |
| --- | --- |
| Mudanças no JSON não aparecem | `--import-realm` não sobrescreve realm existente. [Reimporte](#reimportar-do-zero). |
| `redirect_uri` inválido no login web | Confirme a porta do Vite (5173) e os `redirectUris`/`webOrigins` do `ponto-web`. |
| Admin pula a tela de OTP | Verifique se o usuário tem um dos roles administrativos e se o flow `browser-mfa` está **bound** ao Browser flow. |
| Token sem roles no backend | Garanta o client scope `roles` no `ponto-web` (já incluso) e leia `realm_access.roles`. |
