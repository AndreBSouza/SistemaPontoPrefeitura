# Atualizações automáticas do app (OTA) com Shorebird

O app é **Flutter**. Para empurrar correções **sem esperar a revisão das lojas**, usamos o
**Shorebird** (code push / OTA para Flutter) — sem reescrever nada. Este documento deixa o repo
pronto; falta apenas a parte que depende da **sua conta** (login + app_id).

## O que já está no repositório
- `mobile/shorebird.yaml` — config do Shorebird (com **app_id placeholder** a substituir).
- `mobile/pubspec.yaml` — `shorebird.yaml` declarado em `flutter: assets:` (exigência do Shorebird).
- `mobile/tool/shorebird-release.sh` e `mobile/tool/shorebird-patch.sh` — atalhos dos comandos.

## Passo 1 — pré-requisitos (uma única vez)
1. **Instalar a CLI** (Windows PowerShell):
   ```powershell
   iwr -useb https://raw.githubusercontent.com/shorebirdtech/install/main/install.ps1 | iex
   ```
   (macOS/Linux: `curl --proto '=https' --tlsv1.2 https://raw.githubusercontent.com/shorebirdtech/install/main/install.sh -sSf | bash`)
2. **Login:** `shorebird login` (conta Shorebird — tem tier gratuito + planos pagos).
3. **Registrar o app e gravar o app_id real:** de dentro de `mobile/`:
   ```bash
   shorebird init
   ```
   Isso cria/atualiza o `app_id` no `shorebird.yaml`. Se ele reclamar que já existe (por causa do
   placeholder), rode `shorebird apps create` e **cole o id** retornado no `mobile/shorebird.yaml`,
   ou apague o `shorebird.yaml` atual e rode `shorebird init` de novo.

> A assinatura de release continua a mesma: o Shorebird usa o seu `android/key.properties` /
> `ponto-release-key.jks` normalmente. Nada muda no signing.

## Passo 2 — o fluxo do dia a dia
- **RELEASE** (a cada versão que vai para a loja — substitui o `flutter build`):
  ```bash
  cd mobile && tool/shorebird-release.sh android    # ou: shorebird release android
  ```
  Suba o artefato gerado (AAB/APK) para a Play Store / App Store normalmente.
- **PATCH** (correção OTA sobre o último release — **sem loja**):
  ```bash
  cd mobile && tool/shorebird-patch.sh android       # ou: shorebird patch android
  ```
  Os aparelhos baixam e aplicam o patch **ao abrir o app** (auto_update: true).

## Regra de ouro (o limite do OTA)
O patch só atualiza **código Dart**. **Mudança nativa** — novo plugin, permissão, som/ícone, versão
do engine, ou os assets nativos — **exige um novo `release` + submissão à loja**. (Vale igual para
o EAS Update do Expo: OTA nunca cobre o nativo.)

## Bom saber para o nosso caso
- **Boa parte já se atualiza sem OTA:** regras, branding/cores/logo, comunicados, config de
  verificação vêm da **API** — mudam sem release. O Shorebird cobre o que sobra: **bugs no Dart**.
- **Controle fino (opcional):** para não atualizar automaticamente ao abrir, use
  `auto_update: false` + o pacote [`shorebird_code_push`](https://pub.dev/packages/shorebird_code_push)
  (ex.: só aplicar em Wi-Fi, ou avisar o usuário).
- **Políticas de loja:** o Shorebird foi desenhado para cumprir as regras de Apple/Google (patch de
  correção, sem mudar o propósito do app). Confirme a política vigente antes de publicar.

## CI
Workflow pronto: **`.github/workflows/shorebird-patch.yml`** (acionamento manual — escolhe a
plataforma). Configure os secrets no repositório: `SHOREBIRD_TOKEN` (gere com `shorebird login:ci`)
e a keystore de release (`ANDROID_KEYSTORE_BASE64`, `ANDROID_STORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEY_PASSWORD`). Dispare em Actions → "shorebird-patch" → Run workflow.
