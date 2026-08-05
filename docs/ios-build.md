# Build e publicacao iOS — Ponto Municipal Mobile

Guia para compilar, assinar e publicar o app **Ponto Municipal Mobile** (Flutter) na
plataforma iOS. O build iOS **so pode ser feito em macOS com Xcode** — nao e possivel
compilar para iPhone/iPad a partir de Windows ou Linux. Em ambiente Windows o app Android
builda normalmente; para iOS use um Mac fisico, um Mac na nuvem (ex.: MacStadium) ou a
esteira do Codemagic descrita no final deste documento.

---

## 1. Pre-requisitos

| Item | Versao / observacao |
| --- | --- |
| macOS | 13 (Ventura) ou superior, atualizado |
| Xcode | 15 ou superior (instale pela App Store) |
| Ferramentas de linha de comando do Xcode | `xcode-select --install` |
| CocoaPods | `sudo gem install cocoapods` (ou via Homebrew: `brew install cocoapods`) |
| Flutter SDK | canal `stable`, compativel com `sdk: ^3.10.0` (ver `mobile/pubspec.yaml`) |
| Conta Apple Developer | **paga** (USD 99/ano) para assinatura, TestFlight e App Store |

> Deployment target minimo do app: **iOS 13.0** (definido em `mobile/ios/Podfile` e no
> projeto Xcode `Runner.xcodeproj`). Os plugins `camera` e `geolocator` exigem iOS 13+.

### Permissoes ja configuradas (Info.plist)

O arquivo `mobile/ios/Runner/Info.plist` ja declara as permissoes exigidas pela App Store
para os recursos usados pelo app:

- `NSCameraUsageDescription` — camera para **registro de ponto com biometria facial**
  (reconhecimento e prova de vida).
- `NSLocationWhenInUseUsageDescription` — localizacao no momento do registro para
  **validacao do local de trabalho** (geofencing).
- `NSLocationAlwaysAndWhenInUseUsageDescription` — localizacao continua, necessaria apenas
  se for habilitado o registro automatico por geofencing.

Apps sem essas chaves (com textos claros, em portugues) sao rejeitados na revisao da Apple.

---

## 2. Preparar o projeto

```bash
cd mobile

# Baixar dependencias Dart/Flutter
flutter pub get

# Instalar os pods do iOS
cd ios
pod install
cd ..

# Conferir o ambiente (mostra Xcode, CocoaPods, dispositivos, etc.)
flutter doctor -v
```

> Sempre abra o workspace **`mobile/ios/Runner.xcworkspace`** no Xcode (e nao o
> `Runner.xcodeproj`), pois o projeto usa CocoaPods.

---

## 3. Build de desenvolvimento (debug / device)

```bash
cd mobile

# Listar dispositivos/simuladores disponiveis
flutter devices

# Rodar em um iPhone conectado ou simulador
flutter run -d <id-do-dispositivo>
```

O simulador do iOS **nao possui camera fisica** nem GPS real; para validar biometria
facial e geofencing use um **iPhone fisico**.

---

## 4. Build de release

### 4.1. Build do app (sem empacotar .ipa)

```bash
cd mobile
flutter build ios --release
```

Gera o `Runner.app`. Util para abrir no Xcode e arquivar manualmente (Product > Archive).

### 4.2. Build do IPA (para distribuicao / TestFlight / App Store)

```bash
cd mobile

# Build assinado automaticamente (precisa da conta Apple Developer configurada no Xcode)
flutter build ipa --release

# Ou usando um ExportOptions.plist customizado
flutter build ipa --release --export-options-plist=ios/ExportOptions.plist
```

O `.ipa` final fica em `mobile/build/ios/ipa/`.

---

## 5. Assinatura de codigo (code signing)

A Apple exige que todo app instalado em dispositivo ou enviado a loja seja **assinado**.
Voce precisa de:

1. **Conta Apple Developer** (paga) — <https://developer.apple.com/account>.
2. **Bundle ID** — registre o identificador do app (ex.: `br.gov.ponto.mobile`) em
   *Certificates, Identifiers & Profiles > Identifiers*. O Bundle ID e configurado no
   Xcode em `Runner > Signing & Capabilities` (campo *Bundle Identifier*).
3. **Certificado de distribuicao** (*Apple Distribution*) — criado automaticamente pelo
   Xcode ou manualmente no portal Apple Developer.
4. **Provisioning Profile** — perfil que liga o certificado + Bundle ID + dispositivos.
   Para TestFlight/App Store use um perfil do tipo *App Store*.

### Opcao recomendada: assinatura automatica (Automatic signing)

No Xcode, com o `Runner.xcworkspace` aberto:

1. Selecione o target **Runner** > aba **Signing & Capabilities**.
2. Marque **Automatically manage signing**.
3. Escolha o **Team** (a conta/organizacao Apple Developer da prefeitura).
4. O Xcode cria e gerencia certificados e provisioning profiles sozinho.

### Capacidades necessarias

Habilite no Xcode (*Signing & Capabilities > + Capability*) o que o app usar — por padrao
camera e localizacao **nao** exigem capability extra (apenas as chaves do Info.plist).
Adicione **Push Notifications** / **Background Modes** somente se o app vier a usar esses
recursos.

---

## 6. TestFlight (beta interno e externo)

1. Garanta o app criado no **App Store Connect** (<https://appstoreconnect.apple.com>) com
   o mesmo Bundle ID.
2. Gere e envie o `.ipa`:

   ```bash
   cd mobile
   flutter build ipa --release
   ```

3. Faca o upload do `.ipa` por uma destas vias:
   - **Xcode** — Product > Archive > *Distribute App* > *App Store Connect*; ou
   - **Transporter** (app gratuito da Apple na Mac App Store); ou
   - **linha de comando**:
     ```bash
     xcrun altool --upload-app -f build/ios/ipa/*.ipa -t ios \
       --apple-id <APPLE_ID> --password <APP_SPECIFIC_PASSWORD>
     ```
     (gere uma *app-specific password* em <https://appleid.apple.com>).
4. No App Store Connect > **TestFlight**:
   - **Teste interno**: ate 100 testadores da equipe (membros do time), liberacao
     imediata, sem revisao da Apple.
   - **Teste externo**: ate 10.000 testadores via link/grupo; o primeiro build passa por
     uma revisao rapida da Apple (*Beta App Review*).
5. Builds enviados ao TestFlight tambem podem ser promovidos para producao na App Store.

---

## 7. Publicacao na App Store

1. No App Store Connect, crie a ficha do app (nome, descricao em portugues, capturas de
   tela, classificacao etaria, politica de privacidade).
2. Como o app coleta **dados biometricos (face)** e **localizacao**, preencha com atencao a
   secao **App Privacy** (Privacy Nutrition Labels) declarando esses dados e suas
   finalidades. Tenha a politica de privacidade publicada (alinhada com a LGPD e com o
   DPIA/RIPD em `docs/dpia-ripd.md`).
3. Selecione o build (vindo do TestFlight) e envie para revisao (*Submit for Review*).
4. Apos aprovacao, libere manualmente ou agende a publicacao.

---

## 8. Alternativa sem Mac: Codemagic (CI/CD)

Se a equipe nao tiver Mac disponivel, o **[Codemagic](https://codemagic.io)** roda os
builds iOS em macOS na nuvem e publica direto no TestFlight/App Store. E a forma mais
pratica de compilar iOS a partir de um time que desenvolve em Windows.

Passos gerais:

1. Conecte o repositorio ao Codemagic.
2. Configure a **integracao com a App Store Connect** usando uma **App Store Connect API
   key** (`Issuer ID`, `Key ID` e arquivo `.p8`) — o Codemagic gerencia assinatura e
   upload automaticamente (*automatic code signing*).
3. Defina o workflow Flutter apontando para a pasta `mobile/`.
4. Habilite a publicacao automatica no TestFlight ao final do build.

Exemplo de `codemagic.yaml` (coloque na **raiz do repositorio**):

```yaml
workflows:
  ios-ponto-municipal:
    name: iOS - Ponto Municipal
    instance_type: mac_mini_m2
    max_build_duration: 60
    environment:
      flutter: stable
      xcode: latest
      cocoapods: default
      groups:
        - app_store_credentials   # variaveis da App Store Connect API key
      ios_signing:
        distribution_type: app_store
        bundle_identifier: br.gov.ponto.mobile
    scripts:
      - name: Flutter pub get
        script: cd mobile && flutter pub get
      - name: Pod install
        script: cd mobile/ios && pod install
      - name: Build IPA
        script: |
          cd mobile
          flutter build ipa --release \
            --export-options-plist=/Users/builder/export_options.plist
    artifacts:
      - mobile/build/ios/ipa/*.ipa
    publishing:
      app_store_connect:
        auth: integration
        submit_to_testflight: true
```

> Ajuste `bundle_identifier`, o nome do grupo de credenciais e o tipo de instancia
> conforme a conta. A documentacao oficial do Codemagic para Flutter/iOS detalha a criacao
> da App Store Connect API key e dos grupos de variaveis.

---

## 9. Resumo rapido

```bash
# Em macOS, na pasta mobile/
flutter pub get
cd ios && pod install && cd ..
flutter build ipa --release        # gera build/ios/ipa/*.ipa
# Enviar via Xcode/Transporter/altool -> TestFlight -> App Store
```

Sem Mac? Use o **Codemagic** (secao 8) para compilar e publicar na nuvem.
